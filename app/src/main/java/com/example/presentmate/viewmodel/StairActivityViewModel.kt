package com.example.presentmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.AIServiceFactory
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StairActivityUiState(
    val isLoading: Boolean = false,
    val isClockIn: Boolean = true,
    val suggestedTime: String = "",
    val reason: String = "",
    val confidence: Int = 100,
    val weekdaySchedule: String = "",
    val error: String? = null
)

@HiltViewModel
class StairActivityViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StairActivityUiState())
    val uiState: StateFlow<StairActivityUiState> = _uiState.asStateFlow()
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun analyzeActivity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val platform = AIPreferences.getPlatform(context)
            val apiKey = AIPreferences.getApiKey(context)
            val service = AIServiceFactory.create(platform, apiKey)
            val todayTypical = getTypicalScheduleForToday()

            if (service == null) {
                // Fallback if no AI is configured
                val now = timeFormat.format(Date())
                _uiState.update { it.copy(
                    isLoading = false,
                    suggestedTime = now,
                    confidence = 50,
                    weekdaySchedule = todayTypical,
                    reason = "AI not configured. Defaulting to current time."
                )}
                return@launch
            }

            try {
                val scheduleData = readScheduleCsv()
                val currentTime = timeFormat.format(Date())
                
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"
                val activeSession = withContext(Dispatchers.IO) {
                    attendanceDao.getOngoingSession(uid)
                }
                
                val activeSessionStr = if (activeSession != null) {
                    val timeInFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activeSession.timeIn ?: 0))
                    "The user is CURRENTLY CLOCKED IN (Session started at $timeInFormatted). Therefore, they are almost certainly clocking OUT now."
                } else {
                    "The user is CURRENTLY NOT CLOCKED IN. Therefore, they are almost certainly clocking IN now."
                }

                val prompt = """
                    You are an intelligent assistant for a time-tracking app. 
                    The user just triggered a "stair activity" sensor (indicating they might be arriving at or leaving their study/work location).
                    
                    Here is the user's historical schedule data:
                    $scheduleData
                    
                    Live App State:
                    - Current Time: $currentTime
                    - Active Session Status: $activeSessionStr
                    
                    Based on the schedule and live app state, are they clocking IN (arriving) or clocking OUT (leaving)? 
                    What time should they log? Usually they log the exact time they arrive or leave.
                    Estimate a confidence score (0 to 100) based on how well this aligns with their history and active session.
                    
                    Respond strictly in this format without any markdown blocks or extra text:
                    IS_CLOCK_IN: true/false
                    SUGGESTED_TIME: HH:mm
                    CONFIDENCE: [0-100]
                    REASON: A short 1-sentence reason.
                """.trimIndent()

                val response = service.sendMessage(prompt)
                if (response is AIResponse.Success) {
                    parseResponse(response.message, todayTypical)
                } else if (response is AIResponse.Error) {
                    _uiState.update { it.copy(isLoading = false, error = response.message, weekdaySchedule = todayTypical) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage, weekdaySchedule = todayTypical) }
            }
        }
    }

    private fun parseResponse(message: String, typicalSchedule: String) {
        var isClockIn = true
        var suggestedTime = timeFormat.format(Date())
        var reason = "Couldn't determine based on AI response."
        var confidence = 100

        val lines = message.split("\n")
        for (line in lines) {
            val upperLine = line.uppercase()
            if (upperLine.startsWith("IS_CLOCK_IN:")) {
                isClockIn = upperLine.contains("TRUE")
            } else if (upperLine.startsWith("SUGGESTED_TIME:")) {
                suggestedTime = line.substringAfter(":").trim()
            } else if (upperLine.startsWith("CONFIDENCE:")) {
                confidence = line.substringAfter(":").trim().toIntOrNull() ?: 100
            } else if (upperLine.startsWith("REASON:")) {
                reason = line.substringAfter(":").trim()
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isClockIn = isClockIn,
                suggestedTime = suggestedTime,
                confidence = confidence,
                weekdaySchedule = typicalSchedule,
                reason = reason
            )
        }
    }

    private fun getTypicalScheduleForToday(): String {
        try {
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()) // e.g. "Wednesday"
            val lines = context.assets.open("schedule.csv").bufferedReader().readLines()
            if (lines.size <= 1) return "No typical schedule found."
            
            val matches = lines.drop(1).map { it.split(",") }
                .filter { it.size >= 4 && it[1].equals(dayOfWeek, ignoreCase = true) }
            
            if (matches.isEmpty()) return "No typical schedule for $dayOfWeek."
            
            val entryTimes = matches.map { it[2] }.distinct()
            val exitTimes = matches.map { it[3] }.distinct()
            
            val entryStr = if (entryTimes.size == 1) entryTimes.first() else entryTimes.first()
            val exitStr = if (exitTimes.size == 1) exitTimes.first() else exitTimes.first()
            
            return "$dayOfWeek typical: $entryStr - $exitStr"
        } catch (e: Exception) {
            return "No typical schedule available."
        }
    }

    private suspend fun readScheduleCsv(): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("schedule.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readText()
        } catch (e: Exception) {
            "No schedule data available."
        }
    }

    fun confirmAction(isClockIn: Boolean, timeStr: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"
            val parsedTime = try {
                timeFormat.parse(timeStr)
            } catch (e: Exception) { null }
            
            val timeMillis = parsedTime?.let {
                val now = Date()
                it.apply { 
                    year = now.year
                    month = now.month
                    date = now.date
                }.time
            } ?: System.currentTimeMillis()

            if (isClockIn) {
                // Start a new session
                val record = AttendanceRecord(
                    userId = uid,
                    date = System.currentTimeMillis(),
                    timeIn = timeMillis,
                    timeOut = null
                )
                attendanceDao.insertRecord(record)
            } else {
                // End latest active session
                val activeSession = attendanceDao.getOngoingSession(uid)
                if (activeSession != null) {
                    val updated = activeSession.copy(timeOut = timeMillis)
                    attendanceDao.updateRecord(updated)
                }
            }
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}
