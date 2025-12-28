package com.example.presentmate.ai

import android.graphics.Bitmap
import com.example.presentmate.db.AttendanceRecord
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Service for interacting with Google's Gemini AI
 * Handles text chat and image processing for attendance data extraction
 */
class GeminiService(apiKey: String) {
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 2048
        }
    )
    
    private val chat = model.startChat()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    companion object {
        private const val SYSTEM_PROMPT = """
You are an AI assistant for PresentMate, an attendance tracking app. You help users with:
1. Understanding their attendance data
2. Processing images of attendance sheets to extract records
3. Answering questions about their work schedule

When processing attendance images:
- Extract date, time in, and time out for each row
- Format dates as YYYY-MM-DD
- Format times as HH:MM (24-hour)
- If you find attendance data in an image, output it in this exact format:
  [ATTENDANCE_DATA]
  DATE: YYYY-MM-DD, IN: HH:MM, OUT: HH:MM
  DATE: YYYY-MM-DD, IN: HH:MM, OUT: HH:MM
  [/ATTENDANCE_DATA]
- Always ask for confirmation before suggesting database edits
- Be helpful, concise, and friendly
"""
    }
    
    /**
     * Send a text message to the AI
     */
    suspend fun sendMessage(message: String): AIResponse = withContext(Dispatchers.IO) {
        try {
            val response = chat.sendMessage(message)
            val responseText = response.text ?: "I couldn't generate a response."
            
            // Check if response contains attendance data
            val attendanceRecords = parseAttendanceData(responseText)
            
            AIResponse.Success(
                message = responseText,
                extractedRecords = attendanceRecords
            )
        } catch (e: Exception) {
            AIResponse.Error("Failed to get response: ${e.message}")
        }
    }
    
    /**
     * Send a message with an image for processing
     */
    suspend fun sendMessageWithImage(message: String, image: Bitmap): AIResponse = withContext(Dispatchers.IO) {
        try {
            val prompt = """
$message

If this image contains an attendance sheet or work schedule:
1. Extract all attendance records (date, time in, time out)
2. Format them using the [ATTENDANCE_DATA] tags
3. Summarize what you found

If it's not an attendance sheet, describe what you see.
"""
            
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            
            val responseText = response.text ?: "I couldn't analyze this image."
            val attendanceRecords = parseAttendanceData(responseText)
            
            AIResponse.Success(
                message = responseText,
                extractedRecords = attendanceRecords
            )
        } catch (e: Exception) {
            AIResponse.Error("Failed to process image: ${e.message}")
        }
    }
    
    /**
     * Parse attendance data from AI response
     */
    private fun parseAttendanceData(response: String): List<ParsedAttendance> {
        val records = mutableListOf<ParsedAttendance>()
        
        // Find content between [ATTENDANCE_DATA] tags
        val pattern = """\[ATTENDANCE_DATA\](.*?)\[/ATTENDANCE_DATA\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(response) ?: return records
        
        val dataSection = match.groupValues[1]
        val linePattern = """DATE:\s*(\d{4}-\d{2}-\d{2}),\s*IN:\s*(\d{2}:\d{2}),\s*OUT:\s*(\d{2}:\d{2})""".toRegex()
        
        linePattern.findAll(dataSection).forEach { lineMatch ->
            try {
                val dateStr = lineMatch.groupValues[1]
                val timeInStr = lineMatch.groupValues[2]
                val timeOutStr = lineMatch.groupValues[3]
                
                val date = dateFormat.parse(dateStr)?.time ?: return@forEach
                val timeIn = timeFormat.parse(timeInStr)?.let { 
                    // Combine date with time
                    date + (it.time % (24 * 60 * 60 * 1000))
                } ?: return@forEach
                val timeOut = timeFormat.parse(timeOutStr)?.let {
                    date + (it.time % (24 * 60 * 60 * 1000))
                } ?: return@forEach
                
                records.add(ParsedAttendance(
                    dateStr = dateStr,
                    timeInStr = timeInStr,
                    timeOutStr = timeOutStr,
                    date = date,
                    timeIn = timeIn,
                    timeOut = timeOut
                ))
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }
        
        return records
    }
    
    /**
     * Convert parsed attendance to database records
     */
    fun toAttendanceRecords(parsed: List<ParsedAttendance>): List<AttendanceRecord> {
        return parsed.map { 
            AttendanceRecord(
                date = it.date,
                timeIn = it.timeIn,
                timeOut = it.timeOut
            )
        }
    }
}

/**
 * Represents a parsed attendance entry from AI
 */
data class ParsedAttendance(
    val dateStr: String,
    val timeInStr: String,
    val timeOutStr: String,
    val date: Long,
    val timeIn: Long,
    val timeOut: Long
)

/**
 * AI response sealed class
 */
sealed class AIResponse {
    data class Success(
        val message: String,
        val extractedRecords: List<ParsedAttendance> = emptyList()
    ) : AIResponse()
    
    data class Error(val message: String) : AIResponse()
}
