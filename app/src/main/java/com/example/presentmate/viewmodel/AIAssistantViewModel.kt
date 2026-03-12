package com.example.presentmate.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.AIService
import com.example.presentmate.ai.ParsedAttendance
import com.example.presentmate.db.AttendanceDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Chat message data class
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isFromUser: Boolean,
    val image: Bitmap? = null,
    val extractedRecords: List<ParsedAttendance> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Confirmation state for database edits
 */
sealed class ConfirmationState {
    object None : ConfirmationState()
    data class FirstConfirmation(val records: List<ParsedAttendance>) : ConfirmationState()
    data class SecondConfirmation(val records: List<ParsedAttendance>) : ConfirmationState()
}

/**
 * UI State for AI Assistant
 */
data class AIAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val confirmationState: ConfirmationState = ConfirmationState.None,
    val apiKeyMissing: Boolean = false
)

class AIAssistantViewModel(
    private val attendanceDao: AttendanceDao,
    private val aiService: AIService?
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIAssistantUiState(
        apiKeyMissing = aiService == null
    ))
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()
    
    init {
        if (aiService != null) {
            addMessage(ChatMessage(
                content = "👋 Hi! I'm your AI assistant. I can help you with:\n\n" +
                        "• Understanding your attendance data\n" +
                        "• Processing photos of attendance sheets\n" +
                        "• Adding records to your database (with confirmation)\n\n" +
                        "Just send me a message or share an image!",
                isFromUser = false
            ))
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank() || aiService == null) return
        
        addMessage(ChatMessage(content = text, isFromUser = true))
        val loadingMessage = ChatMessage(content = "Thinking...", isFromUser = false, isLoading = true)
        addMessage(loadingMessage)
        
        viewModelScope.launch {
            when (val response = aiService.sendMessage(text)) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(content = response.message, isFromUser = false, extractedRecords = response.extractedRecords))
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(content = "❌ ${response.message}", isFromUser = false))
                }
            }
        }
    }
    
    fun sendMessageWithImage(text: String, image: Bitmap) {
        if (aiService == null) return
        val messageText = text.ifBlank { "Please analyze this attendance sheet" }
        addMessage(ChatMessage(content = messageText, isFromUser = true, image = image))
        addMessage(ChatMessage(content = "Analyzing image...", isFromUser = false, isLoading = true))
        
        viewModelScope.launch {
            when (val response = aiService.sendMessageWithImage(messageText, image)) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(content = response.message, isFromUser = false, extractedRecords = response.extractedRecords))
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(content = "❌ ${response.message}", isFromUser = false))
                }
            }
        }
    }
    
    private fun promptFirstConfirmation(records: List<ParsedAttendance>) {
        _uiState.update { it.copy(confirmationState = ConfirmationState.FirstConfirmation(records)) }
        addMessage(ChatMessage(content = "📋 I found ${records.size} attendance record(s). Would you like to add them to your database?", isFromUser = false))
    }
    
    fun onFirstConfirmation() {
        val state = _uiState.value.confirmationState
        if (state is ConfirmationState.FirstConfirmation) {
            _uiState.update { it.copy(confirmationState = ConfirmationState.SecondConfirmation(state.records)) }
            addMessage(ChatMessage(
                content = "⚠️ **Final Confirmation**\n\nYou're about to add ${state.records.size} record(s):\n\n" +
                        state.records.take(5).joinToString("\n") { "• ${it.dateStr}: ${it.timeInStr} - ${it.timeOutStr}" } +
                        (if (state.records.size > 5) "\n...and ${state.records.size - 5} more" else "") +
                        "\n\nThis action cannot be undone. Are you sure?",
                isFromUser = false
            ))
        }
    }
    
    fun onSecondConfirmation() {
        val state = _uiState.value.confirmationState
        if (state is ConfirmationState.SecondConfirmation) {
            viewModelScope.launch {
                // Convert parsed records to AttendanceRecord
                val records = state.records.map { parsed ->
                    com.example.presentmate.db.AttendanceRecord(
                        date = parsed.date,
                        timeIn = parsed.timeIn,
                        timeOut = parsed.timeOut
                    )
                }
                records.forEach { attendanceDao.insertRecord(it) }
                _uiState.update { it.copy(confirmationState = ConfirmationState.None) }
                addMessage(ChatMessage(content = "✅ Successfully added ${records.size} record(s) to your database!", isFromUser = false))
            }
        }
    }
    
    fun onCancelConfirmation() {
        _uiState.update { it.copy(confirmationState = ConfirmationState.None) }
        addMessage(ChatMessage(content = "❌ Operation cancelled. No records were added.", isFromUser = false))
    }
    
    private fun addMessage(message: ChatMessage) {
        _uiState.update { state -> state.copy(messages = state.messages + message) }
    }
    
    private fun removeLoadingMessage() {
        _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.isLoading }) }
    }
    
    companion object {
        fun provideFactory(
            attendanceDao: AttendanceDao,
            aiService: AIService?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AIAssistantViewModel(attendanceDao, aiService) as T
            }
        }
    }
}
