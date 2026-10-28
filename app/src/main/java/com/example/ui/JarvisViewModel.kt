package com.example.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.CommandType
import com.example.data.model.JarvisMode
import com.example.data.model.JarvisSettings
import com.example.data.model.MessageSender
import com.example.data.model.SystemStats
import com.example.service.AiBrainService
import com.example.service.CommandDispatcher
import com.example.service.SpeechInputManager
import com.example.service.VoiceEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "JarvisViewModel"

    private val voiceEngine = VoiceEngine(application)
    private val speechInputManager = SpeechInputManager(application)
    private val aiBrainService = AiBrainService()
    private val commandDispatcher = CommandDispatcher(application, aiBrainService)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _jarvisMode = MutableStateFlow(JarvisMode.IDLE)
    val jarvisMode: StateFlow<JarvisMode> = _jarvisMode.asStateFlow()

    private val _statusText = MutableStateFlow("Ready for Command")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _settings = MutableStateFlow(JarvisSettings())
    val settings: StateFlow<JarvisSettings> = _settings.asStateFlow()

    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    val audioRmsLevel: StateFlow<Float> = speechInputManager.rmsLevel
    val isListening: StateFlow<Boolean> = speechInputManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking

    // Event channel for intents (e.g. open browser or exit activity)
    private val _actionIntent = MutableSharedFlow<Intent>()
    val actionIntent: SharedFlow<Intent> = _actionIntent.asSharedFlow()

    private val _shouldExitApp = MutableSharedFlow<Boolean>()
    val shouldExitApp: SharedFlow<Boolean> = _shouldExitApp.asSharedFlow()

    init {
        // Observe speaking state to update mode
        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                if (speaking) {
                    _jarvisMode.value = JarvisMode.SPEAKING
                    _statusText.value = "Speaking to ${settings.value.ownerName}…"
                } else if (_jarvisMode.value == JarvisMode.SPEAKING) {
                    _jarvisMode.value = JarvisMode.IDLE
                    _statusText.value = "Ready for Command"
                }
            }
        }

        // Post welcome message and speak
        postInitialGreeting()
    }

    private fun postInitialGreeting() {
        val welcome = "Welcome back, ${settings.value.ownerName}. ${settings.value.jarvisName} is online. How can I help you today?"
        val welcomeMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = welcome,
            commandType = CommandType.STATUS
        )
        _messages.value = listOf(welcomeMsg)

        viewModelScope.launch {
            // Short delay so TTS engine finishes initializing
            kotlinx.coroutines.delay(800)
            voiceEngine.speak(welcome)
        }
    }

    fun startListening() {
        if (_jarvisMode.value == JarvisMode.SPEAKING) {
            voiceEngine.stop()
        }

        _jarvisMode.value = JarvisMode.LISTENING
        _statusText.value = "Listening to ${settings.value.ownerName}…"

        speechInputManager.startListening(
            languagePref = settings.value.preferredLanguage,
            onResult = { recognizedText ->
                _jarvisMode.value = JarvisMode.PROCESSING
                _statusText.value = "Processing: \"$recognizedText\""
                handleUserCommand(recognizedText)
            },
            onError = { errorMessage ->
                _jarvisMode.value = JarvisMode.IDLE
                _statusText.value = errorMessage
            }
        )
    }

    fun stopListening() {
        speechInputManager.stopListening()
        if (_jarvisMode.value == JarvisMode.LISTENING) {
            _jarvisMode.value = JarvisMode.IDLE
            _statusText.value = "Ready for Command"
        }
    }

    fun toggleListening() {
        if (isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun handleUserCommand(command: String) {
        val cleanCommand = command.trim()
        if (cleanCommand.isBlank()) return

        // Add User Message
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = cleanCommand
        )
        _messages.value = _messages.value + userMsg

        _jarvisMode.value = JarvisMode.PROCESSING
        _statusText.value = "JARVIS Thinking…"

        viewModelScope.launch {
            try {
                val result = commandDispatcher.processCommand(
                    command = cleanCommand,
                    ownerName = settings.value.ownerName,
                    customApiKey = settings.value.customApiKey
                )

                val jarvisMsg = ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = result.replyText,
                    commandType = result.commandType,
                    actionUrl = result.actionUrl
                )
                _messages.value = _messages.value + jarvisMsg

                _jarvisMode.value = JarvisMode.SPEAKING
                _statusText.value = "Replying…"

                // Speak reply
                voiceEngine.speak(result.replyText)

                // Launch intent if requested
                result.intentToLaunch?.let { intent ->
                    _actionIntent.emit(intent)
                }

                if (result.shouldExit) {
                    kotlinx.coroutines.delay(2000)
                    _shouldExitApp.emit(true)
                }
            } catch (e: Exception) {
                Log.e(tag, "Command execution failed", e)
                val errorMsg = ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "Sorry ${settings.value.ownerName}, an error occurred while executing that command.",
                    commandType = CommandType.NONE
                )
                _messages.value = _messages.value + errorMsg
                _jarvisMode.value = JarvisMode.ERROR
                _statusText.value = "System Error"
                voiceEngine.speak(errorMsg.text)
            }
        }
    }

    fun replayMessage(message: ChatMessage) {
        voiceEngine.speak(message.text)
    }

    fun stopSpeaking() {
        voiceEngine.stop()
        _jarvisMode.value = JarvisMode.IDLE
        _statusText.value = "Ready for Command"
    }

    fun toggleMute() {
        val newMuted = !settings.value.isMuted
        val updated = settings.value.copy(isMuted = newMuted)
        _settings.value = updated
        voiceEngine.updateSettings(updated.speechRate, updated.speechPitch, updated.isMuted)
    }

    fun updateSettings(newSettings: JarvisSettings) {
        _settings.value = newSettings
        voiceEngine.updateSettings(newSettings.speechRate, newSettings.speechPitch, newSettings.isMuted)
    }

    fun clearChat() {
        _messages.value = emptyList()
        aiBrainService.clearHistory()
        val resetMsg = ChatMessage(
            sender = MessageSender.SYSTEM,
            text = "Conversation memory cleared. System reset to idle state."
        )
        _messages.value = listOf(resetMsg)
    }

    fun testSpeech() {
        voiceEngine.speak("Speech test initialized. All vocal synthesis modules are operating normally, ${settings.value.ownerName}.")
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
        speechInputManager.destroy()
    }
}
