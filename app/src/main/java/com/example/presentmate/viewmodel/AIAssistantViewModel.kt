package com.example.presentmate.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.GeminiService
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
    apiKey: String?
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIAssistantUiState(
        apiKeyMissing = apiKey.isNullOrBlank()
    ))
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()
    
    private var geminiService: GeminiService? = null
    
    init {
        if (!apiKey.isNullOrBlank()) {
            geminiService = GeminiService(apiKey)
            // Add welcome message
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
        if (text.isBlank() || geminiService == null) return
        
        // Add user message
        addMessage(ChatMessage(content = text, isFromUser = true))
        
        // Show loading
        val loadingMessage = ChatMessage(
            content = "Thinking...",
            isFromUser = false,
            isLoading = true
        )
        addMessage(loadingMessage)
        
        viewModelScope.launch {
            when (val response = geminiService?.sendMessage(text)) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(
                        content = response.message,
                        isFromUser = false,
                        extractedRecords = response.extractedRecords
                    ))
                    
                    // If records were found, prompt for confirmation
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(
                        content = "❌ ${response.message}",
                        isFromUser = false
                    ))
                }
                null -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(
                        content = "❌ AI service not available",
                        isFromUser = false
                    ))
                }
            }
        }
    }
    
    fun sendMessageWithImage(text: String, image: Bitmap) {
        if (geminiService == null) return
        
        val messageText = text.ifBlank { "Please analyze this attendance sheet" }
        
        // Add user message with image
        addMessage(ChatMessage(
            content = messageText,
            isFromUser = true,
            image = image
        ))
        
        // Show loading
        addMessage(ChatMessage(
            content = "Analyzing image...",
            isFromUser = false,
            isLoading = true
        ))
        
        viewModelScope.launch {
            when (val response = geminiService?.sendMessageWithImage(messageText, image)) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(
                        content = response.message,
                        isFromUser = false,
                        extractedRecords = response.extractedRecords
                    ))
                    
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    addMessage(ChatMessage(
                        content = "❌ ${response.message}",
                        isFromUser = false
                    ))
                }
                null -> {
                    removeLoadingMessage()
                }
            }
        }
    }
    
    private fun promptFirstConfirmation(records: List<ParsedAttendance>) {
        _uiState.update { it.copy(confirmationState = ConfirmationState.FirstConfirmation(records)) }
        addMessage(ChatMessage(
            content = "📋 I found ${records.size} attendance record(s). Would you like to add them to your database?",
            isFromUser = false
        ))
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
                val records = geminiService?.toAttendanceRecords(state.records) ?: emptyList()
                records.forEach { record ->
                    attendanceDao.insertRecord(record)
                }
                
                _uiState.update { it.copy(confirmationState = ConfirmationState.None) }
                addMessage(ChatMessage(
                    content = "✅ Successfully added ${records.size} record(s) to your database!",
                    isFromUser = false
                ))
            }
        }
    }
    
    fun onCancelConfirmation() {
        _uiState.update { it.copy(confirmationState = ConfirmationState.None) }
        addMessage(ChatMessage(
            content = "❌ Operation cancelled. No records were added.",
            isFromUser = false
        ))
    }
    
    private fun addMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }
    
    private fun removeLoadingMessage() {
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.isLoading })
        }
    }
    
    companion object {
        fun provideFactory(
            attendanceDao: AttendanceDao,
            apiKey: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AIAssistantViewModel(attendanceDao, apiKey) as T
            }
        }
    }
}
