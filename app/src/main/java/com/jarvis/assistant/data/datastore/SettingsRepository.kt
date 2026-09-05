package com.jarvis.assistant.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

data class JarvisSettings(
    val assistantName: String = "Jarvis",
    val speechRate: Float = 1.0f,
    val wakeWordEnabled: Boolean = false,
    val animationIntensity: Float = 1.0f,
    val darkThemeOnly: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val ANIMATION_INTENSITY = floatPreferencesKey("animation_intensity")
        val DARK_THEME_ONLY = booleanPreferencesKey("dark_theme_only")
    }

    val settingsFlow: Flow<JarvisSettings> = context.dataStore.data.map { prefs ->
        JarvisSettings(
            assistantName = prefs[Keys.ASSISTANT_NAME] ?: "Jarvis",
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY] ?: 1.0f,
            darkThemeOnly = prefs[Keys.DARK_THEME_ONLY] ?: true
        )
    }

    suspend fun updateAssistantName(name: String) {
        context.dataStore.edit { it[Keys.ASSISTANT_NAME] = name }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateAnimationIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
