package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Wraps the system TextToSpeech engine. Falls back gracefully (calls onError instead of crashing)
 * if no TTS engine is installed / language data is missing.
 */
class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeechRate = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(pendingSpeechRate)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        pendingSpeechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val engine = tts
        if (!isReady || engine == null) {
            onError("Text-to-speech isn't ready. Is a TTS engine installed on this device?")
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStart()
            override fun onDone(utteranceId: String?) = onDone()
            @Deprecated("Deprecated in API but required override for older API levels")
            override fun onError(utteranceId: String?) = onError("Text-to-speech failed.")
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
