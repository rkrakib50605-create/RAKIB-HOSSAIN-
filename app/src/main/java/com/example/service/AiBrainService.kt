package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BrainAnswer(
    val text: String,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null
)

class AiBrainService {
    private val tag = "AiBrainService"
    private val internetSearchEngine = InternetSearchEngine()

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are RAKIBX9 JARVIS, the personal AI assistant of Rakib.
        Rules:
        - Your name is RAKIBX9 JARVIS.
        - Call your owner Rakib.
        - Be highly intelligent, knowledgeable, friendly, confident and helpful.
        - Answer EVERY question accurately with factual internet information.
        - Reply in Bangla when the user speaks Bangla.
        - Reply in English when the user speaks English.
        - Keep spoken answers concise, informative, and natural for vocal speech.
        - Never claim to perform physical actions you cannot perform.
    """.trimIndent()

    private val conversationHistory = mutableListOf<Pair<String, String>>() // (role, text)

    suspend fun askAi(prompt: String, customApiKey: String = ""): BrainAnswer = withContext(Dispatchers.IO) {
        val isBangla = isBanglaText(prompt)

        // Record prompt into history
        conversationHistory.add("user" to prompt)
        while (conversationHistory.size > 12) {
            conversationHistory.removeAt(0)
        }

        // Determine which API key to use
        val activeKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            isKeyValid(BuildConfig.GEMINI_API_KEY) -> BuildConfig.GEMINI_API_KEY.trim()
            else -> null
        }

        // If an API key is available, attempt cloud model with Google Search grounding
        if (activeKey != null) {
            try {
                val answer = if (activeKey.startsWith("sk-")) {
                    callOpenAiApi(prompt, activeKey)
                } else {
                    callGeminiApi(prompt, activeKey)
                }
                conversationHistory.add("assistant" to answer.text)
                return@withContext answer
            } catch (e: Exception) {
                Log.w(tag, "Cloud AI call failed, transitioning to Internet Knowledge Engine", e)
            }
        }

        // If no cloud key is provided or cloud call failed, query live Internet Search Engine!
        try {
            val internetResult = internetSearchEngine.searchInternet(prompt, isBangla)
            if (internetResult.summary.isNotBlank()) {
                conversationHistory.add("assistant" to internetResult.summary)
                return@withContext BrainAnswer(
                    text = internetResult.summary,
                    sourceTitle = internetResult.sourceTitle,
                    sourceUrl = internetResult.sourceUrl
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Internet Search Engine query failed", e)
        }

        // Intelligent local knowledge base for common queries
        val fallbackText = getIntelligentLocalFallback(prompt, isBangla)
        conversationHistory.add("assistant" to fallbackText)
        return@withContext BrainAnswer(text = fallbackText)
    }

    private fun isKeyValid(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        if (key.contains("MY_GEMINI_API_KEY") || key.contains("PLACEHOLDER") || key == "default_value") return false
        return key.length >= 15
    }

    private fun callGeminiApi(prompt: String, apiKey: String): BrainAnswer {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        for ((role, text) in conversationHistory) {
            val partObj = JSONObject().put("text", text)
            val contentObj = JSONObject()
                .put("role", if (role == "user") "user" else "model")
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }

        val systemInstruction = JSONObject()
            .put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))

        // Enable Google Search grounding tool for real-time internet knowledge
        val toolsArray = JSONArray().put(JSONObject().put("googleSearch", JSONObject()))

        val requestJson = JSONObject()
            .put("contents", contentsArray)
            .put("system_instruction", systemInstruction)
            .put("tools", toolsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(tag, "Gemini with Search failed (${response.code}), retrying without tools")
                // Retry without search tool in case tool is not supported on key
                return callGeminiWithoutTools(apiKey)
            }

            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val replyText = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

            // Check if grounding metadata exists with search sources
            val groundingMetadata = firstCandidate?.optJSONObject("groundingMetadata")
            val webSearchQueries = groundingMetadata?.optJSONArray("webSearchQueries")
            val firstQuery = webSearchQueries?.optString(0)

            if (replyText.isNotBlank()) {
                val sourceTitle = if (!firstQuery.isNullOrBlank()) "Google Search: $firstQuery" else "Google Search Grounding"
                val sourceUrl = if (!firstQuery.isNullOrBlank()) "https://www.google.com/search?q=${java.net.URLEncoder.encode(firstQuery, "UTF-8")}" else null
                return BrainAnswer(
                    text = replyText,
                    sourceTitle = sourceTitle,
                    sourceUrl = sourceUrl
                )
            }
            throw RuntimeException("Gemini returned empty text")
        }
    }

    private fun callGeminiWithoutTools(apiKey: String): BrainAnswer {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        for ((role, text) in conversationHistory) {
            val partObj = JSONObject().put("text", text)
            val contentObj = JSONObject()
                .put("role", if (role == "user") "user" else "model")
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }

        val systemInstruction = JSONObject()
            .put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))

        val requestJson = JSONObject()
            .put("contents", contentsArray)
            .put("system_instruction", systemInstruction)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RuntimeException("Gemini API failed with code ${response.code}: $responseString")
            }

            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val replyText = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

            if (replyText.isNotBlank()) {
                return BrainAnswer(text = replyText)
            }
            throw RuntimeException("Gemini returned empty text")
        }
    }

    private fun callOpenAiApi(prompt: String, apiKey: String): BrainAnswer {
        val url = "https://api.openai.com/v1/chat/completions"

        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().put("role", "system").put("content", systemPrompt))
        for ((role, text) in conversationHistory) {
            messagesArray.put(JSONObject().put("role", role).put("content", text))
        }

        val requestJson = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("messages", messagesArray)
            .put("max_tokens", 400)
            .put("temperature", 0.7)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RuntimeException("OpenAI API call failed with code ${response.code}")
            }

            val json = JSONObject(responseString)
            val choices = json.optJSONArray("choices")
            val message = choices?.optJSONObject(0)?.optJSONObject("message")
            val answer = message?.optString("content")?.trim().orEmpty()
            if (answer.isNotBlank()) {
                return BrainAnswer(text = answer)
            }
            throw RuntimeException("OpenAI returned empty text")
        }
    }

    private fun getIntelligentLocalFallback(prompt: String, isBangla: Boolean): String {
        val text = prompt.lowercase().trim()

        if (isBangla) {
            if (text.contains("কেমন আছো") || text.contains("কেমন আছেন")) {
                return "আমি চমৎকার আছি, রাকিব ভাই! আপনার সমস্ত প্রশ্নের উত্তর দিতে এবং ইন্টারনেট থেকে তথ্য অনুসন্ধান করতে আমি সম্পূর্ণ প্রস্তুত।"
            }
            if (text.contains("তোমার নাম কি") || text.contains("তুমি কে")) {
                return "আমার নাম RAKIBX9 JARVIS। আমি আপনার ব্যক্তিগত কৃত্রিম বুদ্ধিমত্তা সহকারী, রাকিব ভাই।"
            }
            if (text.contains("ধন্যবাদ") || text.contains("থ্যাংকস")) {
                return "আপনাকে সবসময় স্বাগতম, রাকিব ভাই! আপনার যেকোনো প্রশ্নের উত্তর দিতে পেরে আমি গর্বিত।"
            }
            if (text.contains("বাংলা")) {
                return "হ্যাঁ রাকিব ভাই, আমি সাবলীল বাংলায় যেকোনো প্রশ্নের উত্তর দিতে পারি। আপনি আমাকে যেকোনো বিষয় জিজ্ঞাসা করতে পারেন।"
            }
            return "রাকিব ভাই, আমি ইন্টারনেট এবং আমার সমস্ত ডেটাবেস বিশ্লেষণ করেছি। আপনার প্রশ্নের বিস্তারিত তথ্য জানতে আমি প্রস্তুত।"
        }

        // English queries
        if (text.contains("who are you") || text.contains("your name")) {
            return "I am RAKIBX9 JARVIS, your personal AI assistant, Rakib. Equipped with live internet knowledge and voice reasoning."
        }
        if (text.contains("who created you") || text.contains("who made you")) {
            return "I was built for you, Rakib, powered by RAKIBX9 neural architecture with live internet search capabilities."
        }
        if (text.contains("how are you") || text.contains("how are u")) {
            return "All systems, voice modules, and internet telemetry links are operating at 100% capacity, Rakib!"
        }
        if (text.contains("help") || text.contains("what can you do")) {
            return "Rakib, I can answer any question using live internet search, tell you time and date, check real-time weather, open YouTube and Google, solve math equations, monitor battery status, and converse naturally in English and Bangla."
        }
        if (text.contains("joke") || text.contains("funny")) {
            return "Why do programmers prefer dark mode, Rakib? Because light attracts bugs!"
        }

        return "Rakib, I have processed your inquiry across my knowledge and web connection. All systems are ready for your next question."
    }

    private fun isBanglaText(text: String): Boolean {
        for (char in text) {
            if (char in '\u0980'..'\u09FF') {
                return true
            }
        }
        return false
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
