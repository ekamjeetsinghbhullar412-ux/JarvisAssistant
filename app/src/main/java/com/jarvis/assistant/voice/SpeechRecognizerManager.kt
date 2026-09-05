package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

sealed class SpeechEvent {
    object ReadyForSpeech : SpeechEvent()
    data class PartialResult(val text: String) : SpeechEvent()
    data class FinalResult(val text: String) : SpeechEvent()
    data class Error(val message: String) : SpeechEvent()
    object Done : SpeechEvent()
}

/**
 * Thin wrapper around [SpeechRecognizer]. Requires RECORD_AUDIO permission to already be granted —
 * this class does not request permissions itself (that's a UI-layer responsibility).
 */
class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(onEvent: (SpeechEvent) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onEvent(SpeechEvent.Error("Speech recognition isn't available on this device."))
            return
        }

        stopListening() // ensure no duplicate sessions
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    onEvent(SpeechEvent.ReadyForSpeech)
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) onEvent(SpeechEvent.PartialResult(text))
                }

                override fun onResults(results: android.os.Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        onEvent(SpeechEvent.FinalResult(text))
                    } else {
                        onEvent(SpeechEvent.Error("I didn't catch that."))
                    }
                    onEvent(SpeechEvent.Done)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that — try again?"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Network issue with speech recognition."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Microphone permission is required."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "One moment, still processing."
                        else -> "Speech recognition error ($error)."
                    }
                    onEvent(SpeechEvent.Error(message))
                    onEvent(SpeechEvent.Done)
                }

                override fun onRmsChanged(rmsdB: Float) { /* used by waveform UI if wired up */ }
                override fun onBeginningOfSpeech() {}
                override fun onEndOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            startListening(intent)
        }
    }

    fun stopListening() {
        recognizer?.destroy()
        recognizer = null
    }
}
