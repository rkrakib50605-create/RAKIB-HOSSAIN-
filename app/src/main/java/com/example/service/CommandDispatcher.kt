package com.example.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.MediaStore
import com.example.data.model.CommandType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandResult(
    val replyText: String,
    val commandType: CommandType,
    val actionUrl: String? = null,
    val intentToLaunch: Intent? = null,
    val shouldExit: Boolean = false
)

class CommandDispatcher(
    private val context: Context,
    private val aiBrainService: AiBrainService
) {
    suspend fun processCommand(
        command: String,
        ownerName: String = "Rakib",
        customApiKey: String = ""
    ): CommandResult {
        val text = command.lowercase().trim()

        // 1. EXIT
        if (text in listOf("exit", "quit", "stop jarvis", "goodbye jarvis", "bye jarvis", "বন্ধ হও")) {
            val reply = if (isBanglaText(command)) {
                "বিদায় $ownerName ভাই। JARVIS সাইন অফ করছে।"
            } else {
                "Goodbye $ownerName. JARVIS signing off."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.EXIT,
                shouldExit = true
            )
        }

        // 2. TIME
        if (text.contains("what time") || text.contains("the time") || text == "time" ||
            text.contains("কয়টা বাজে") || text.contains("সময় কত") || text.contains("বর্তমান সময়")
        ) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val currentTime = timeFormat.format(Date())

            val reply = if (isBanglaText(command)) {
                "$ownerName ভাই, এখন সময় $currentTime।"
            } else {
                "$ownerName, the time is $currentTime."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.TIME
            )
        }

        // 3. DATE
        if (text.contains("what is today") || text.contains("today's date") || text == "date" ||
            text == "today" || text.contains("আজকের তারিখ") || text.contains("তারিখ কত")
        ) {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
            val todayDate = dateFormat.format(Date())

            val reply = if (isBanglaText(command)) {
                "$ownerName ভাই, আজকের তারিখ $todayDate।"
            } else {
                "$ownerName, today's date is $todayDate."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.DATE
            )
        }

        // 4. YOUTUBE
        if (text.contains("open youtube") || text == "youtube" || text.contains("ইউটিউব খোলো") || text.contains("ইউটিউব")) {
            val url = "https://www.youtube.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val reply = if (isBanglaText(command)) {
                "ইউটিউব ওপেন করছি, $ownerName ভাই।"
            } else {
                "Opening YouTube, $ownerName."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.YOUTUBE,
                actionUrl = url,
                intentToLaunch = intent
            )
        }

        // 5. GOOGLE
        if (text.contains("open google") || text == "google" || text.contains("গুগল খোলো") || text.contains("গুগল")) {
            val url = "https://www.google.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val reply = if (isBanglaText(command)) {
                "গুগল ওপেন করছি, $ownerName ভাই।"
            } else {
                "Opening Google, $ownerName."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.GOOGLE,
                actionUrl = url,
                intentToLaunch = intent
            )
        }

        // 6. CAMERA
        if (text.contains("open camera") || text == "camera" || text.contains("ক্যামেরা খোলো") || text.contains("ক্যামেরা")) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val reply = if (isBanglaText(command)) {
                "$ownerName ভাই, আপনার জন্য ক্যামেরা চালু করছি।"
            } else {
                "Opening camera for you, $ownerName."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.CAMERA,
                intentToLaunch = intent
            )
        }

        // 7. BATTERY
        if (text.contains("battery") || text.contains("charge") || text.contains("ব্যাটারি")) {
            val batteryStatus = getBatteryLevel(context)
            val reply = if (isBanglaText(command)) {
                "$ownerName ভাই, আপনার ডিভাইসের ব্যাটারি লেভেল ${batteryStatus.first}% ${if (batteryStatus.second) "(চার্জ হচ্ছে)" else ""}।"
            } else {
                "$ownerName, battery level is at ${batteryStatus.first}% ${if (batteryStatus.second) "and charging." else "on battery power."}"
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.BATTERY
            )
        }

        // 8. SYSTEM STATUS
        if (text.contains("system status") || text.contains("diagnostics") || text.contains("status") || text.contains("সিস্টেম স্ট্যাটাস")) {
            val reply = if (isBanglaText(command)) {
                "$ownerName ভাই, RAKIBX9 JARVIS এর সমস্ত কোর সিস্টেম অনলাইনে রয়েছে এবং স্বাভাবিকভাবে কাজ করছে।"
            } else {
                "All RAKIBX9 JARVIS core systems are online and fully operational, $ownerName."
            }
            return CommandResult(
                replyText = reply,
                commandType = CommandType.STATUS
            )
        }

        // 9. AI BRAIN & LIVE INTERNET QUESTION ANSWERING
        val answer = aiBrainService.askAi(command, customApiKey)
        return CommandResult(
            replyText = answer.text,
            commandType = CommandType.AI_CHAT,
            actionUrl = answer.sourceUrl
        )
    }

    private fun getBatteryLevel(context: Context): Pair<Int, Boolean> {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, iFilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Pair(batteryPct, isCharging)
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
