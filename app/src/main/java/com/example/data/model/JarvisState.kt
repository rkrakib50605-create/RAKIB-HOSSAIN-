package com.example.data.model

enum class JarvisMode {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class SystemStats(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkConnected: Boolean = true,
    val memoryUsageMb: Long = 0,
    val uptimeFormatted: String = "00:00:00"
)

data class JarvisSettings(
    val ownerName: String = "Rakib",
    val jarvisName: String = "RAKIBX9 JARVIS",
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 1.0f,
    val isMuted: Boolean = false,
    val preferredLanguage: String = "AUTO", // AUTO, EN, BN
    val customApiKey: String = ""
)
