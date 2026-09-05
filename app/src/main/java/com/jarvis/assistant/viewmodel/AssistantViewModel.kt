package com.jarvis.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.ai.AIRepository
import com.jarvis.assistant.ai.AIResult
import com.jarvis.assistant.commands.Command
import com.jarvis.assistant.commands.CommandExecutor
import com.jarvis.assistant.commands.CommandParser
import com.jarvis.assistant.data.datastore.SettingsRepository
import com.jarvis.assistant.data.local.AppDatabase
import com.jarvis.assistant.data.local.ConversationEntity
import com.jarvis.assistant.model.AssistantState
import com.jarvis.assistant.model.Message
import com.jarvis.assistant.voice.SpeechEvent
import com.jarvis.assistant.voice.SpeechRecognizerManager
import com.jarvis.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A command that needs the user to explicitly confirm before it runs
 * (e.g. "call Mom", "text Dana ..."), surfaced to the UI as a yes/no prompt.
 */
data class PendingConfirmation(val command: Command, val confirmationPrompt: String)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechRecognizerManager(application)
    private val ttsManager = TextToSpeechManager(application)
    private val commandExecutor = CommandExecutor(application)
    private val aiRepository = AIRepository()
    private val settingsRepository = SettingsRepository(application)
    private val db = AppDatabase.getInstance(application)

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { ttsManager.setSpeechRate(it.speechRate) }
        }
        viewModelScope.launch {
            db.conversationDao().observeAll().collect { entities ->
                _messages.value = entities.map {
                    Message(it.id, Message.Role.valueOf(it.role), it.text, it.timestamp)
                }
            }
        }
    }

    fun startListening() {
        _state.value = AssistantState.LISTENING
        _transcript.value = ""
        speechManager.startListening { event ->
            when (event) {
                is SpeechEvent.ReadyForSpeech -> _state.value = AssistantState.LISTENING
                is SpeechEvent.PartialResult -> _transcript.value = event.text
                is SpeechEvent.FinalResult -> handleUserUtterance(event.text)
                is SpeechEvent.Error -> {
                    _state.value = AssistantState.ERROR
                    speak(event.message)
                }
                SpeechEvent.Done -> Unit
            }
        }
    }

    fun stopListening() {
        speechManager.stopListening()
        if (_state.value == AssistantState.LISTENING) _state.value = AssistantState.IDLE
    }

    /** Called when the user types instead of speaking (fallback / accessibility path). */
    fun submitTypedText(text: String) = handleUserUtterance(text)

    private fun handleUserUtterance(text: String) {
        _transcript.value = text
        addMessage(Message.Role.USER, text)

        val command = CommandParser.parse(text)
        if (command is Command.Unrecognized) {
            askAI(text)
            return
        }

        if (command.requiresConfirmation) {
            _state.value = AssistantState.IDLE
            _pendingConfirmation.value = PendingConfirmation(
                command = command,
                confirmationPrompt = confirmationPromptFor(command)
            )
            speak(_pendingConfirmation.value!!.confirmationPrompt)
            return
        }

        runCommand(command)
    }

    fun confirmPendingCommand() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        runCommand(pending.command)
    }

    fun cancelPendingCommand() {
        _pendingConfirmation.value = null
        speak("Okay, cancelled.")
    }

    private fun runCommand(command: Command) {
        _state.value = AssistantState.THINKING
        val result = commandExecutor.execute(command)
        addMessage(Message.Role.ASSISTANT, result.spokenResponse)
        speak(result.spokenResponse)
    }

    private fun askAI(userText: String) {
        _state.value = AssistantState.THINKING
        viewModelScope.launch {
            when (val result = aiRepository.sendConversation(_messages.value)) {
                is AIResult.Success -> {
                    addMessage(Message.Role.ASSISTANT, result.reply)
                    speak(result.reply)
                }
                is AIResult.Failure -> {
                    _state.value = AssistantState.ERROR
                    speak(result.userMessage)
                }
            }
        }
    }

    private fun speak(text: String) {
        _state.value = AssistantState.SPEAKING
        ttsManager.speak(
            text = text,
            onDone = { _state.value = AssistantState.IDLE },
            onError = { _state.value = AssistantState.IDLE }
        )
    }

    private fun addMessage(role: Message.Role, text: String) {
        viewModelScope.launch {
            db.conversationDao().insert(ConversationEntity(role = role.name, text = text, timestamp = System.currentTimeMillis()))
        }
    }

    fun clearConversation() {
        viewModelScope.launch { db.conversationDao().clearAll() }
    }

    private fun confirmationPromptFor(command: Command): String = when (command) {
        is Command.CallContact -> "Call ${command.name}? Say yes to confirm."
        is Command.SendMessage -> "Send \"${command.body}\" to ${command.name}? Say yes to confirm."
        Command.ClearHistory -> "Clear all conversation history? Say yes to confirm."
        else -> "Are you sure?"
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
