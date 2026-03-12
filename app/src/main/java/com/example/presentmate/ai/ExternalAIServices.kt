package com.example.presentmate.ai

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
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
    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        callChatApi(
            url = "https://openrouter.ai/api/v1/chat/completions",
            apiKey = apiKey,
            model = "meta-llama/llama-3.2-3b-instruct:free",
            systemPrompt = SYSTEM_PROMPT,
            userMessage = message,
            temperature = temperature,
            maxTokens = maxTokens,
            extraHeaders = mapOf("HTTP-Referer" to "https://presentmate.app")
        )
    }

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with OpenRouter in this version. Please switch to Gemini for image processing.")
}

// ─── Groq ──────────────────────────────────────────────────────────────────────
class GroqService(private val apiKey: String, private val temperature: Float, private val maxTokens: Int) : AIService {
    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
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

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with Groq. Please switch to Gemini for image processing.")
}

// ─── Cohere ────────────────────────────────────────────────────────────────────
class CohereService(private val apiKey: String, private val temperature: Float, private val maxTokens: Int) : AIService {
    override suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.cohere.com/v2/chat")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
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
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val content = json.getJSONObject("message")
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                val records = parseAttendanceData(content)
                AIResponse.Success(message = content, extractedRecords = records)
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                AIResponse.Error("Cohere API error ${conn.responseCode}: $error")
            }
        } catch (e: Exception) {
            AIResponse.Error("Failed to reach Cohere: ${e.message}")
        }
    }

    override suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse =
        AIResponse.Error("Image analysis is not supported with Cohere. Please switch to Gemini for image processing.")
}

// ─── Shared HTTP helper ────────────────────────────────────────────────────────
private fun callChatApi(
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
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
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
            val responseText = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(responseText)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            val records = parseAttendanceData(content)
            AIResponse.Success(message = content, extractedRecords = records)
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown"
            AIResponse.Error("API error ${conn.responseCode}: $error")
        }
    } catch (e: Exception) {
        AIResponse.Error("Network error: ${e.message}")
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
