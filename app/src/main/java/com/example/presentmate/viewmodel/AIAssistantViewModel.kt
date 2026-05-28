package com.example.presentmate.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.AIService
import com.example.presentmate.ai.AIServiceFactory
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.ParsedAttendance
import com.example.presentmate.db.AttendanceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Chat message data class.
 *
 * Note: images are stored as URI (not Bitmap) to avoid holding large
 * bitmaps in the ViewModel StateFlow, which would cause OOM errors as the
 * chat history grows.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val imageUri: Uri? = null,       // URI reference only — decode to Bitmap at render time
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

/**
 * ViewModel for the AI Assistant screen.
 *
 * Injected via Hilt (@HiltViewModel) so the full Hilt graph is used.
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val attendanceDao: AttendanceDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    private fun getAiService(): AIService? {
        val platform    = AIPreferences.getPlatform(context)
        val apiKey      = AIPreferences.getApiKey(context)
        val temperature = AIPreferences.getTemperature(context)
        val maxTokens   = AIPreferences.getMaxTokens(context)
        return AIServiceFactory.create(platform, apiKey, temperature, maxTokens)
    }

    init {
        refreshAiServiceState()
    }

    fun refreshAiServiceState() {
        val service = getAiService()
        val isMissing = service == null
        _uiState.update { it.copy(apiKeyMissing = isMissing) }

        if (!isMissing && _uiState.value.messages.isEmpty()) {
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
        val service = getAiService()
        if (text.isBlank() || service == null) return

        addMessage(ChatMessage(content = text, isFromUser = true))
        addMessage(ChatMessage(content = "Thinking...", isFromUser = false, isLoading = true))

        viewModelScope.launch {
            val response = service.sendMessage(text)
            when (response) {
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
                    addMessage(ChatMessage(content = "❌ ${response.message}", isFromUser = false))
                }
            }
        }
    }

    /**
     * Send a message with an image identified by its [imageUri].
     * The actual Bitmap decoding is done inside the coroutine, not stored in state.
     */
    fun sendMessageWithImage(text: String, imageUri: Uri, resolveBitmap: suspend (Uri) -> android.graphics.Bitmap?) {
        val service = getAiService()
        if (service == null) return
        val messageText = text.ifBlank { "Please analyze this attendance sheet" }
        addMessage(ChatMessage(content = messageText, isFromUser = true, imageUri = imageUri))
        addMessage(ChatMessage(content = "Analyzing image...", isFromUser = false, isLoading = true))

        viewModelScope.launch {
            val bitmap = resolveBitmap(imageUri)
            val response = if (bitmap != null) {
                service.sendMessageWithImage(messageText, bitmap)
            } else {
                AIResponse.Error("Could not decode the selected image.")
            }
            when (response) {
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
                    addMessage(ChatMessage(content = "❌ ${response.message}", isFromUser = false))
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
                val records = state.records.map { parsed ->
                    com.example.presentmate.db.AttendanceRecord(userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned", 
                        date = parsed.date,
                        timeIn = parsed.timeIn,
                        timeOut = parsed.timeOut
                    )
                }
                records.forEach { attendanceDao.insertRecord(it) }
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
        addMessage(ChatMessage(content = "❌ Operation cancelled. No records were added.", isFromUser = false))
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { state -> state.copy(messages = state.messages + message) }
    }

    private fun removeLoadingMessage() {
        _uiState.update { state -> state.copy(messages = state.messages.filterNot { it.isLoading }) }
    }
}
