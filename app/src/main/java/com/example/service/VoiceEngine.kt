package com.example.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private val tag = "VoiceEngine"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRate: Float = 1.05f
    private var speechPitch: Float = 1.0f
    private var isMuted: Boolean = false

    private val localeEnglish = Locale.US
    private val localeBangla = Locale.Builder().setLanguage("bn").setRegion("BD").build()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(localeEnglish)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(tag, "English TTS voice missing or unsupported")
            }
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.e(tag, "TTS Error code: $errorCode for $utteranceId")
                }
            })
            isInitialized = true
            Log.d(tag, "TextToSpeech initialized successfully")
        } else {
            Log.e(tag, "TextToSpeech initialization failed with status $status")
        }
    }

    fun updateSettings(rate: Float, pitch: Float, muted: Boolean) {
        speechRate = rate
        speechPitch = pitch
        isMuted = muted
        if (isInitialized) {
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)
        }
        if (muted) {
            stop()
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (isMuted || text.isBlank()) {
            _isSpeaking.value = false
            onFinished?.invoke()
            return
        }

        if (!isInitialized) {
            Log.w(tag, "TTS not initialized yet")
            return
        }

        val containsBangla = isBanglaText(text)
        val selectedLocale = if (containsBangla) localeBangla else localeEnglish

        val langResult = tts?.setLanguage(selectedLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default
            tts?.language = Locale.getDefault()
        }

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }

    private fun isBanglaText(text: String): Boolean {
        for (char in text) {
            if (char in '\u0980'..'\u09FF') {
                return true
            }
        }
        return false
    }
}
