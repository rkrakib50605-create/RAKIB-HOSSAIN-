package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechInputManager(private val context: Context) {
    private val tag = "SpeechInputManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } else {
            Log.w(tag, "Speech recognition is not available on this device")
        }
    }

    fun startListening(
        languagePref: String = "AUTO",
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onResultCallback = onResult
        onErrorCallback = onError

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    when (languagePref) {
                        "BN" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                        "EN" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        else -> {
                            // Support multi-language with Bengali and English
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-BD"))
                        }
                    }
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e(tag, "Failed to start speech recognition", e)
                _isListening.value = false
                onErrorCallback?.invoke("Could not start microphone: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(tag, "Failed to stop listening", e)
            } finally {
                _isListening.value = false
                _rmsLevel.value = 0f
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(tag, "Failed to destroy recognizer", e)
            } finally {
                speechRecognizer = null
                _isListening.value = false
                _rmsLevel.value = 0f
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize roughly between 0.0 and 1.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsLevel.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsLevel.value = 0f

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                SpeechRecognizer.ERROR_NETWORK -> "Network error connecting to speech service."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly, Rakib."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Trying again…"
                SpeechRecognizer.ERROR_SERVER -> "Speech server error occurred."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything, Rakib."
                else -> "Speech recognition error ($error)"
            }
            Log.w(tag, "Speech recognition error: $errorMessage")
            onErrorCallback?.invoke(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrBlank()) {
                onResultCallback?.invoke(recognizedText)
            } else {
                onErrorCallback?.invoke("Sorry Rakib, I couldn't understand.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()
            if (!partialText.isNullOrBlank()) {
                // Keep listening active
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
