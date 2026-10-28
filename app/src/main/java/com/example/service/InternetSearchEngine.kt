package com.example.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class InternetSearchResult(
    val summary: String,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
    val isSuccess: Boolean = true
)

class InternetSearchEngine {
    private val tag = "InternetSearchEngine"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun searchInternet(query: String, isBangla: Boolean = false): InternetSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)

        // 1. Check if it's a Weather query
        if (isWeatherQuery(cleanQuery)) {
            val weatherResult = fetchLiveWeather(cleanQuery, isBangla)
            if (weatherResult != null) {
                return@withContext weatherResult
            }
        }

        // 2. Check if it's a Math/Calculation query
        val mathResult = tryEvaluateMath(cleanQuery)
        if (mathResult != null) {
            val reply = if (isBangla) {
                "রাকিব ভাই, গণনার ফলাফল হলো: $mathResult"
            } else {
                "Rakib, the calculation result is $mathResult"
            }
            return@withContext InternetSearchResult(
                summary = reply,
                sourceTitle = "Calculator Engine"
            )
        }

        // 3. Query Wikipedia (Bangla or English)
        val wikiResult = fetchWikipedia(cleanQuery, isBangla)
        if (wikiResult != null && wikiResult.summary.isNotBlank()) {
            return@withContext wikiResult
        }

        // 4. Query DuckDuckGo Instant Answer API
        val ddgResult = fetchDuckDuckGo(cleanQuery)
        if (ddgResult != null && ddgResult.summary.isNotBlank()) {
            return@withContext ddgResult
        }

        // 5. Fallback Web Search Result
        val encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.toString())
        val searchUrl = "https://www.google.com/search?q=$encoded"
        val fallbackText = if (isBangla) {
            "রাকিব ভাই, আমি ইন্টারনেটে '$cleanQuery' সম্পর্কিত তথ্য খুঁজছি। আপনি নিচের লিঙ্কে ক্লিক করে সরাসরি বিস্তারিত দেখতে পারেন।"
        } else {
            "Rakib, I browsed the internet for '$cleanQuery'. You can view full real-time web results via the link below."
        }

        return@withContext InternetSearchResult(
            summary = fallbackText,
            sourceTitle = "Google Web Search",
            sourceUrl = searchUrl
        )
    }

    private fun isWeatherQuery(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("weather") || q.contains("temperature") || q.contains("forecast") ||
                q.contains("আবহাওয়া") || q.contains("তাপমাত্রা") || q.contains("বৃষ্টি")
    }

    private fun fetchLiveWeather(query: String, isBangla: Boolean): InternetSearchResult? {
        try {
            // Extract city if present, or default to Dhaka / local
            var city = "Dhaka"
            val words = query.split(" ", ",", "?")
            for (i in 0 until words.size - 1) {
                if (words[i].lowercase() in listOf("in", "at", "for", "of") && words[i + 1].isNotBlank()) {
                    city = words[i + 1].replace("?", "").trim()
                    break
                }
            }
            if (query.lowercase().contains("chittagong") || query.contains("চট্টগ্রাম")) city = "Chittagong"
            if (query.lowercase().contains("sylhet") || query.contains("সিলেট")) city = "Sylhet"
            if (query.lowercase().contains("london")) city = "London"
            if (query.lowercase().contains("new york")) city = "New York"
            if (query.lowercase().contains("tokyo")) city = "Tokyo"

            val encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString())
            val url = "https://wttr.in/$encodedCity?format=%C:+%t+(feels+like+%f),+humidity+%h,+wind+%w"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "curl/7.68.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val weatherData = response.body?.string()?.trim().orEmpty()
                    if (weatherData.isNotBlank() && !weatherData.contains("Unknown location")) {
                        val reply = if (isBangla) {
                            "রাকিব ভাই, $city শহরের বর্তমান লাইভ আবহাওয়া হলো: $weatherData।"
                        } else {
                            "Rakib, the current live weather in $city is: $weatherData."
                        }
                        return InternetSearchResult(
                            summary = reply,
                            sourceTitle = "Live Weather ($city)",
                            sourceUrl = "https://wttr.in/$encodedCity"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch live weather", e)
        }
        return null
    }

    private fun fetchWikipedia(query: String, isBangla: Boolean): InternetSearchResult? {
        val lang = if (isBangla) "bn" else "en"
        try {
            // Step A: Search for matching page title
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val searchApi = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&utf8="

            val searchReq = Request.Builder()
                .url(searchApi)
                .header("User-Agent", "RakibX9JarvisApp/1.0 (rkrakib50605@gmail.com)")
                .build()

            var targetTitle: String? = null
            client.newCall(searchReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val searchList = json.optJSONObject("query")?.optJSONArray("search")
                    if (searchList != null && searchList.length() > 0) {
                        targetTitle = searchList.getJSONObject(0).optString("title")
                    }
                }
            }

            if (targetTitle.isNullOrBlank()) {
                targetTitle = query.split(" ").take(3).joinToString("_")
            }

            // Step B: Fetch Summary of the article
            val encodedTitle = URLEncoder.encode(targetTitle!!.replace(" ", "_"), StandardCharsets.UTF_8.toString())
            val summaryApi = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"

            val summaryReq = Request.Builder()
                .url(summaryApi)
                .header("User-Agent", "RakibX9JarvisApp/1.0 (rkrakib50605@gmail.com)")
                .build()

            client.newCall(summaryReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val extract = json.optString("extract")
                    val pageTitle = json.optString("title")
                    val pageUrl = json.optJSONObject("content_urls")
                        ?.optJSONObject("desktop")
                        ?.optString("page")

                    if (!extract.isNullOrBlank()) {
                        // Shorten to first 2-3 sentences for concise speech
                        val conciseExtract = makeConcise(extract)
                        val reply = if (isBangla) {
                            "রাকিব ভাই, ইন্টারনেট উইকিপিডিয়া অনুযায়ী $pageTitle সম্পর্কে:\n$conciseExtract"
                        } else {
                            "Rakib, according to internet Wikipedia regarding $pageTitle:\n$conciseExtract"
                        }
                        return InternetSearchResult(
                            summary = reply,
                            sourceTitle = "Wikipedia: $pageTitle",
                            sourceUrl = pageUrl ?: "https://$lang.wikipedia.org/wiki/$encodedTitle"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch Wikipedia summary for $query", e)
        }
        return null
    }

    private fun fetchDuckDuckGo(query: String): InternetSearchResult? {
        try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RakibX9JarvisApp/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)

                    val abstractText = json.optString("AbstractText")
                    val answer = json.optString("Answer")
                    val definition = json.optString("Definition")
                    val heading = json.optString("Heading")
                    val abstractUrl = json.optString("AbstractURL")

                    val chosenText = when {
                        answer.isNotBlank() -> answer
                        abstractText.isNotBlank() -> abstractText
                        definition.isNotBlank() -> definition
                        else -> null
                    }

                    if (!chosenText.isNullOrBlank()) {
                        val concise = makeConcise(chosenText)
                        val reply = "Rakib, based on internet results for $heading:\n$concise"
                        return InternetSearchResult(
                            summary = reply,
                            sourceTitle = "DuckDuckGo: $heading",
                            sourceUrl = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encoded"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch DuckDuckGo for $query", e)
        }
        return null
    }

    private fun tryEvaluateMath(query: String): String? {
        try {
            val cleaned = query.lowercase()
                .replace("what is", "")
                .replace("calculate", "")
                .replace("equals", "")
                .replace("=", "")
                .replace("times", "*")
                .replace("x", "*")
                .replace("multiplied by", "*")
                .replace("plus", "+")
                .replace("minus", "-")
                .replace("divided by", "/")
                .trim()

            // Match simple expressions like "25 * 40" or "100 + 450"
            val pattern = Pattern.compile("^([0-9.]+)\\s*([+\\-*/])\\s*([0-9.]+)$")
            val matcher = pattern.matcher(cleaned)
            if (matcher.find()) {
                val num1 = matcher.group(1)?.toDoubleOrNull() ?: return null
                val op = matcher.group(2) ?: return null
                val num2 = matcher.group(3)?.toDoubleOrNull() ?: return null

                val res = when (op) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*" -> num1 * num2
                    "/" -> if (num2 != 0.0) num1 / num2 else return "Cannot divide by zero"
                    else -> return null
                }
                return if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(res)
            }
        } catch (e: Exception) {
            // Ignore math eval errors
        }
        return null
    }

    private fun sanitizeQuery(query: String): String {
        return query
            .replace("search the internet for", "", ignoreCase = true)
            .replace("search internet for", "", ignoreCase = true)
            .replace("search for", "", ignoreCase = true)
            .replace("browse for", "", ignoreCase = true)
            .replace("tell me about", "", ignoreCase = true)
            .replace("what is", "", ignoreCase = true)
            .replace("who is", "", ignoreCase = true)
            .replace("where is", "", ignoreCase = true)
            .replace("who was", "", ignoreCase = true)
            .replace("ইন্টারনেটে খোঁজো", "", ignoreCase = true)
            .replace("সম্পর্কে বলো", "", ignoreCase = true)
            .replace("কী", "", ignoreCase = true)
            .trim()
            .ifBlank { query.trim() }
    }

    private fun makeConcise(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        return if (sentences.size > 3) {
            sentences.take(3).joinToString(" ")
        } else {
            text
        }
    }
}
