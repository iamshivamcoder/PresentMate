package com.example.presentmate.ai

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private const val SYSTEM_PROMPT = """
You are an AI assistant for PresentMate, an attendance tracking app. Help users:
1. Understand their attendance data
2. Process attendance sheets (extract date, time in, time out)
3. Answer questions about their schedule

When you find attendance data, format it as:
[ATTENDANCE_DATA]
DATE: YYYY-MM-DD, IN: HH:MM, OUT: HH:MM
[/ATTENDANCE_DATA]

Always ask confirmation before suggesting database edits. Be concise and friendly.
"""

// ─── OpenRouter ────────────────────────────────────────────────────────────────
class OpenRouterService(private val apiKey: String, private val temperature: Float, private val maxTokens: Int) : AIService {
    private val models = listOf(
        "meta-llama/llama-3.2-3b-instruct:free",
        "google/gemma-2-9b-it:free",
        "mistralai/mistral-7b-instruct:free"
    )

    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        var lastResponse: AIResponse = AIResponse.Error("No OpenRouter models succeeded.")
        for (modelName in models) {
            val response = withApiRetry {
                callChatApi(
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = apiKey,
                    model = modelName,
                    systemPrompt = SYSTEM_PROMPT,
                    userMessage = message,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    extraHeaders = mapOf("HTTP-Referer" to "https://presentmate.app")
                )
            }
            if (response is AIResponse.Success) {
                return@withContext response
            }
            lastResponse = response
        }
        lastResponse
    }

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with OpenRouter in this version. Please switch to Gemini for image processing.")
}

// ─── Groq ──────────────────────────────────────────────────────────────────────
class GroqService(private val apiKey: String, private val temperature: Float, private val maxTokens: Int) : AIService {
    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        withApiRetry {
            callChatApi(
                url = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = apiKey,
                model = "llama-3.3-70b-versatile",
                systemPrompt = SYSTEM_PROMPT,
                userMessage = message,
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
    }

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with Groq. Please switch to Gemini for image processing.")
}

// ─── Cohere ────────────────────────────────────────────────────────────────────
class CohereService(private val apiKey: String, private val temperature: Float, private val maxTokens: Int) : AIService {
    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        withApiRetry {
            try {
                val url = URL("https://api.cohere.com/v2/chat")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 60_000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.setRequestProperty("Accept-Encoding", "gzip")
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("model", "command-r-08-2024")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT.trim())
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", message)
                        })
                    })
                    put("temperature", temperature.toDouble())
                    put("max_tokens", maxTokens)
                }

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                if (conn.responseCode == 200) {
                    val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                        java.util.zip.GZIPInputStream(conn.inputStream)
                    } else conn.inputStream
                    val response = stream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val content = json.getJSONObject("message")
                        .getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                    val records = parseAttendanceData(content)
                    AIResponse.Success(message = content, extractedRecords = records)
                } else {
                    val stream = conn.errorStream ?: conn.inputStream
                    val errorRaw = try {
                        if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                            java.util.zip.GZIPInputStream(stream).bufferedReader().readText()
                        } else stream.bufferedReader().readText()
                    } catch (e: Exception) { "" }
                    val parsedError = extractErrorMessage(errorRaw)
                    val displayMessage = when (conn.responseCode) {
                        429 -> "Rate limit exceeded. Please wait a moment before trying again.\n\nDetails: $parsedError"
                        401 -> "Invalid API key. Please check your key in Settings."
                        else -> "Cohere API Error ${conn.responseCode}: $parsedError"
                    }
                    AIResponse.Error(displayMessage)
                }
            } catch (e: Exception) {
                AIResponse.Error("Failed to reach Cohere: ${e.message}")
            }
        }
    }

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with Cohere. Please switch to Gemini for image processing.")
}

// ─── Shared HTTP helper ────────────────────────────────────────────────────────
private suspend fun callChatApi(
    url: String,
    apiKey: String,
    model: String,
    systemPrompt: String,
    userMessage: String,
    temperature: Float,
    maxTokens: Int,
    extraHeaders: Map<String, String> = emptyMap()
): AIResponse {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 60_000
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Accept-Encoding", "gzip")
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.doOutput = true

        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt.trim()) })
                put(JSONObject().apply { put("role", "user"); put("content", userMessage) })
            })
            put("temperature", temperature.toDouble())
            put("max_tokens", maxTokens)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        if (conn.responseCode == 200) {
            val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else conn.inputStream
            
            val responseText = stream.bufferedReader().readText()
            val json = JSONObject(responseText)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            val records = parseAttendanceData(content)
            AIResponse.Success(message = content, extractedRecords = records)
        } else {
            val stream = conn.errorStream ?: conn.inputStream
            val errorRaw = try {
                if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                    java.util.zip.GZIPInputStream(stream).bufferedReader().readText()
                } else stream.bufferedReader().readText()
            } catch (e: Exception) { "" }
            
            val parsedError = extractErrorMessage(errorRaw)
            val displayMessage = when (conn.responseCode) {
                429 -> "Rate limit exceeded. Please wait a moment before trying again.\n\nDetails: $parsedError"
                401 -> "Invalid API key. Please check your key in Settings."
                else -> "API Error ${conn.responseCode}: $parsedError"
            }
            AIResponse.Error(displayMessage)
        }
    } catch (e: Exception) {
        AIResponse.Error("Network error: ${e.message}")
    }
}

private suspend fun withApiRetry(block: suspend () -> AIResponse): AIResponse {
    var currentDelay = 1000L
    var lastResponse: AIResponse = AIResponse.Error("Unknown fallback error")

    for (attempt in 1..3) {
        val response = try {
            block()
        } catch (e: Exception) {
            AIResponse.Error("Network error: ${e.message}")
        }
        lastResponse = response

        if (response is AIResponse.Error) {
            val isRateLimit = response.message.contains("Rate limit") || response.message.contains("429")
            val isServerError = response.message.contains("Error 50") || response.message.contains("502") || response.message.contains("503")
            
            if ((isRateLimit || isServerError) && attempt < 3) {
                delay(currentDelay)
                currentDelay *= 2
                continue
            }
        }
        return response
    }
    return lastResponse
}

private fun extractErrorMessage(errorRaw: String): String {
    if (errorRaw.isBlank()) return "Unknown error"
    return try {
        val json = JSONObject(errorRaw)
        val errorObj = json.optJSONObject("error") ?: return errorRaw.take(200)
        val metadata = errorObj.optJSONObject("metadata")
        val rawMsg = metadata?.optString("raw")
        val errMsg = errorObj.optString("message")
        
        rawMsg?.takeIf { it.isNotBlank() } 
            ?: errMsg.takeIf { it.isNotBlank() } 
            ?: errorObj.toString().take(200)
    } catch (e: Exception) {
        errorRaw.take(200)
    }
}

private fun parseAttendanceData(response: String): List<ParsedAttendance> {
    val records = mutableListOf<ParsedAttendance>()
    val pattern = """\[ATTENDANCE_DATA\](.*?)\[/ATTENDANCE_DATA\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val match = pattern.find(response) ?: return records
    val linePattern = """DATE:\s*(\d{4}-\d{2}-\d{2}),\s*IN:\s*(\d{2}:\d{2}),\s*OUT:\s*(\d{2}:\d{2})""".toRegex()
    linePattern.findAll(match.groupValues[1]).forEach { m ->
        try {
            val dateStr = m.groupValues[1]; val timeInStr = m.groupValues[2]; val timeOutStr = m.groupValues[3]
            val date = dateFormat.parse(dateStr)?.time ?: return@forEach
            val timeIn = timeFormat.parse(timeInStr)?.let { date + (it.time % 86400000) } ?: return@forEach
            val timeOut = timeFormat.parse(timeOutStr)?.let { date + (it.time % 86400000) } ?: return@forEach
            records.add(ParsedAttendance(dateStr, timeInStr, timeOutStr, date, timeIn, timeOut))
        } catch (_: Exception) {}
    }
    return records
}
