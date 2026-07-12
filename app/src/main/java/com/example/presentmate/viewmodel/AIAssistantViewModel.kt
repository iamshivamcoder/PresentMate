package com.example.presentmate.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.AIService
import com.example.presentmate.ai.AIServiceFactory
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.ParsedAttendance
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.ChatMessageEntity
import com.example.presentmate.db.ChatSession
import com.example.presentmate.db.ChatSessionDao
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
@androidx.compose.runtime.Stable
data class AIAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val confirmationState: ConfirmationState = ConfirmationState.None,
    val apiKeyMissing: Boolean = false,
    val currentSessionId: String? = null
)

/**
 * ViewModel for the AI Assistant screen.
 *
 * Injected via Hilt (@HiltViewModel) so the full Hilt graph is used.
 * Supports persistent chat sessions — messages are saved to Room so they
 * survive app restarts, mirroring the ChatGPT-style UX.
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val chatSessionDao: ChatSessionDao,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    private val uid: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unassigned"

    private fun getAiService(): AIService? {
        val platform    = AIPreferences.getPlatform(context)
        val apiKey      = AIPreferences.getApiKey(context)
        val temperature = AIPreferences.getTemperature(context)
        val maxTokens   = AIPreferences.getMaxTokens(context)
        return AIServiceFactory.create(platform, apiKey, temperature, maxTokens)
    }

    init {
        val sessionId = savedStateHandle.get<String>("sessionId")
        if (sessionId != null) {
            loadSession(sessionId)
        } else {
            refreshAiServiceState()
        }
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

    /** Load an existing session and its messages from DB */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val service = getAiService()
            val isMissing = service == null
            val messages = chatSessionDao.getMessages(sessionId).map { entity ->
                ChatMessage(
                    id = entity.id,
                    content = entity.content,
                    isFromUser = entity.isFromUser,
                    imageUri = entity.imageUriString?.let { Uri.parse(it) }
                )
            }
            _uiState.update { it.copy(
                messages = messages,
                currentSessionId = sessionId,
                apiKeyMissing = isMissing
            )}
        }
    }

    /** Start a completely new session, clearing the current messages */
    fun startNewSession() {
        _uiState.update { it.copy(
            messages = emptyList(),
            currentSessionId = null,
            confirmationState = ConfirmationState.None
        )}
        refreshAiServiceState()
    }

    /** Delete a session and all its messages from the DB */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatSessionDao.deleteMessagesForSession(sessionId)
            chatSessionDao.deleteSession(sessionId)
        }
    }

    private suspend fun ensureSession(firstUserMessage: String): String {
        val existing = _uiState.value.currentSessionId
        if (existing != null) {
            // Update lastMessageAt
            chatSessionDao.getSession(existing)?.let {
                chatSessionDao.updateSession(it.copy(lastMessageAt = System.currentTimeMillis()))
            }
            return existing
        }
        val newId = UUID.randomUUID().toString()
        val title = firstUserMessage.trim().take(40).let { if (it.length == 40) "$it…" else it }
        chatSessionDao.insertSession(ChatSession(
            id = newId,
            userId = uid,
            title = title,
            createdAt = System.currentTimeMillis(),
            lastMessageAt = System.currentTimeMillis()
        ))
        _uiState.update { it.copy(currentSessionId = newId) }
        return newId
    }

    private suspend fun persistMessage(sessionId: String, message: ChatMessage) {
        if (message.isLoading) return  // don't persist transient loading bubbles
        chatSessionDao.insertMessage(ChatMessageEntity(
            id = message.id,
            sessionId = sessionId,
            userId = uid,
            content = message.content,
            isFromUser = message.isFromUser,
            imageUriString = message.imageUri?.toString(),
            createdAt = System.currentTimeMillis()
        ))
    }

    fun sendMessage(text: String) {
        val service = getAiService()
        if (text.isBlank() || service == null) return

        val userMsg = ChatMessage(content = text, isFromUser = true)
        val loadingMsg = ChatMessage(content = "Thinking...", isFromUser = false, isLoading = true)
        addMessage(userMsg)
        addMessage(loadingMsg)

        viewModelScope.launch {
            val sessionId = ensureSession(text)
            persistMessage(sessionId, userMsg)

            val contextualPrompt = buildContextualPrompt(text)
            val response = service.sendMessage(contextualPrompt)
            when (response) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    val aiMsg = ChatMessage(
                        content = response.message,
                        isFromUser = false,
                        extractedRecords = response.extractedRecords
                    )
                    addMessage(aiMsg)
                    persistMessage(sessionId, aiMsg)
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    val errMsg = ChatMessage(content = "❌ ${response.message}", isFromUser = false)
                    addMessage(errMsg)
                    persistMessage(sessionId, errMsg)
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
        val userMsg = ChatMessage(content = messageText, isFromUser = true, imageUri = imageUri)
        val loadingMsg = ChatMessage(content = "Analyzing image...", isFromUser = false, isLoading = true)
        addMessage(userMsg)
        addMessage(loadingMsg)

        viewModelScope.launch {
            val sessionId = ensureSession(messageText)
            persistMessage(sessionId, userMsg)

            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                resolveBitmap(imageUri)
            }
            val response = if (bitmap != null) {
                val contextualPrompt = buildContextualPrompt(messageText)
                service.sendMessageWithImage(contextualPrompt, bitmap)
            } else {
                AIResponse.Error("Could not decode the selected image.")
            }
            when (response) {
                is AIResponse.Success -> {
                    removeLoadingMessage()
                    val aiMsg = ChatMessage(
                        content = response.message,
                        isFromUser = false,
                        extractedRecords = response.extractedRecords
                    )
                    addMessage(aiMsg)
                    persistMessage(sessionId, aiMsg)
                    if (response.extractedRecords.isNotEmpty()) {
                        promptFirstConfirmation(response.extractedRecords)
                    }
                }
                is AIResponse.Error -> {
                    removeLoadingMessage()
                    val errMsg = ChatMessage(content = "❌ ${response.message}", isFromUser = false)
                    addMessage(errMsg)
                    persistMessage(sessionId, errMsg)
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
                    com.example.presentmate.db.AttendanceRecord(userId = uid,
                        date = parsed.date,
                        timeIn = parsed.timeIn,
                        timeOut = parsed.timeOut
                    )
                }
                attendanceDao.insertAll(records)
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

    private suspend fun buildContextualPrompt(userText: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val allRecords = attendanceDao.getAllRecordsNonFlow(uid).take(50)
            if (allRecords.isNotEmpty()) {
                val dataStr = allRecords.joinToString(separator = "\n") {
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.date))
                    val timeInStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.timeIn ?: it.date))
                    val timeOutStr = it.timeOut?.let { t -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(t)) } ?: "Ongoing"
                    "${dateStr}: ${timeInStr} - ${timeOutStr}"
                }
                "[System Context: Here is the user's latest attendance data for reference:\n$dataStr]\n\nUser Message: $userText"
            } else {
                userText
            }
        }
    }
}
