package com.jarvis.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.datastore.JarvisSettings
import com.jarvis.assistant.data.datastore.SettingsRepository
import com.jarvis.assistant.data.local.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val db = AppDatabase.getInstance(application)

    val settings: StateFlow<JarvisSettings> = repository.settingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), JarvisSettings()
    )

    fun updateAssistantName(name: String) = viewModelScope.launch { repository.updateAssistantName(name) }
    fun updateSpeechRate(rate: Float) = viewModelScope.launch { repository.updateSpeechRate(rate) }
    fun updateWakeWordEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateWakeWordEnabled(enabled) }
    fun updateAnimationIntensity(intensity: Float) = viewModelScope.launch { repository.updateAnimationIntensity(intensity) }

    fun clearConversationHistory() = viewModelScope.launch { db.conversationDao().clearAll() }
    fun resetAllSettings() = viewModelScope.launch { repository.clearAll() }
}
