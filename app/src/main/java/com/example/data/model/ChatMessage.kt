package com.example.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

enum class CommandType {
    NONE,
    TIME,
    DATE,
    YOUTUBE,
    GOOGLE,
    CAMERA,
    BATTERY,
    STATUS,
    EXIT,
    AI_CHAT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandType: CommandType = CommandType.NONE,
    val actionUrl: String? = null,
    val isSpoken: Boolean = false
)
