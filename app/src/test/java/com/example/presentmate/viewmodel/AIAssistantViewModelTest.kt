package com.example.presentmate.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.presentmate.ai.AIPlatform
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.AIResponse
import com.example.presentmate.ai.AIService
import com.example.presentmate.ai.AIServiceFactory
import com.example.presentmate.ai.ParsedAttendance
import com.example.presentmate.db.AttendanceDao
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AIAssistantViewModelTest {

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    @MockK
    private lateinit var aiService: AIService

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        coEvery { attendanceDao.insertRecord(any()) } returns Unit

        // Mock the static-like objects so we can control AIService creation
        mockkObject(AIPreferences)
        mockkObject(AIServiceFactory)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun init_withNullService_setsApiKeyMissing() = runTest {
        every { AIPreferences.getPlatform(any()) } returns AIPlatform.GEMINI
        every { AIPreferences.getApiKey(any()) } returns ""
        every { AIPreferences.getTemperature(any()) } returns 0.7f
        every { AIPreferences.getMaxTokens(any()) } returns 1024
        every { AIServiceFactory.create(any(), any(), any(), any()) } returns null

        val viewModel = AIAssistantViewModel(attendanceDao, context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.apiKeyMissing)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun init_withService_addsWelcomeMessage() = runTest {
        every { AIPreferences.getPlatform(any()) } returns AIPlatform.GEMINI
        every { AIPreferences.getApiKey(any()) } returns "test-key"
        every { AIPreferences.getTemperature(any()) } returns 0.7f
        every { AIPreferences.getMaxTokens(any()) } returns 1024
        every { AIServiceFactory.create(any(), any(), any(), any()) } returns aiService

        val viewModel = AIAssistantViewModel(attendanceDao, context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.apiKeyMissing)
        assertEquals(1, state.messages.size)
        assertFalse(state.messages[0].isFromUser)
    }

    @Test
    fun sendMessage_addsUserMessageAndGetsResponse() = runTest {
        every { AIPreferences.getPlatform(any()) } returns AIPlatform.GEMINI
        every { AIPreferences.getApiKey(any()) } returns "test-key"
        every { AIPreferences.getTemperature(any()) } returns 0.7f
        every { AIPreferences.getMaxTokens(any()) } returns 1024
        every { AIServiceFactory.create(any(), any(), any(), any()) } returns aiService

        val mockResponse = AIResponse.Success("This is AI response", emptyList())
        coEvery { aiService.sendMessage("Hello") } returns mockResponse

        val viewModel = AIAssistantViewModel(attendanceDao, context)
        advanceUntilIdle() // Process init message

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size) // Welcome, User Hello, AI Response
        assertEquals("Hello", state.messages[1].content)
        assertTrue(state.messages[1].isFromUser)
        assertEquals("This is AI response", state.messages[2].content)
        assertFalse(state.messages[2].isFromUser)
    }

    @Test
    fun handleConfirmation_goesThroughFlowAndCommits() = runTest {
        every { AIPreferences.getPlatform(any()) } returns AIPlatform.GEMINI
        every { AIPreferences.getApiKey(any()) } returns "test-key"
        every { AIPreferences.getTemperature(any()) } returns 0.7f
        every { AIPreferences.getMaxTokens(any()) } returns 1024
        every { AIServiceFactory.create(any(), any(), any(), any()) } returns aiService

        val parsedRecords = listOf(ParsedAttendance("2023-10-27", "09:00", "17:00", 0L, 0L, 0L))
        val mockResponse = AIResponse.Success("Found", parsedRecords)
        coEvery { aiService.sendMessage(any()) } returns mockResponse

        val viewModel = AIAssistantViewModel(attendanceDao, context)
        advanceUntilIdle()

        viewModel.sendMessage("Test")
        advanceUntilIdle()

        // Verify FirstConfirmation State
        assertTrue(viewModel.uiState.value.confirmationState is ConfirmationState.FirstConfirmation)

        // Confirm first time
        viewModel.onFirstConfirmation()

        // Verify SecondConfirmation State
        assertTrue(viewModel.uiState.value.confirmationState is ConfirmationState.SecondConfirmation)

        // Confirm second time
        viewModel.onSecondConfirmation()
        advanceUntilIdle()

        // Verify committed to DB and state reset
        coVerify(exactly = 1) { attendanceDao.insertRecord(any()) }
        assertEquals(ConfirmationState.None, viewModel.uiState.value.confirmationState)

        // Verify success message was added
        val lastMessage = viewModel.uiState.value.messages.last()
        assertTrue(lastMessage.content.contains("Successfully"))
    }

    @Test
    fun cancelConfirmation_resetsState() = runTest {
        every { AIPreferences.getPlatform(any()) } returns AIPlatform.GEMINI
        every { AIPreferences.getApiKey(any()) } returns "test-key"
        every { AIPreferences.getTemperature(any()) } returns 0.7f
        every { AIPreferences.getMaxTokens(any()) } returns 1024
        every { AIServiceFactory.create(any(), any(), any(), any()) } returns aiService

        val parsedRecords = listOf(ParsedAttendance("2023-10-27", "09:00", "17:00", 0L, 0L, 0L))
        val mockResponse = AIResponse.Success("Found", parsedRecords)
        coEvery { aiService.sendMessage(any()) } returns mockResponse

        val viewModel = AIAssistantViewModel(attendanceDao, context)
        advanceUntilIdle()

        viewModel.sendMessage("Test")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.confirmationState is ConfirmationState.FirstConfirmation)

        viewModel.onCancelConfirmation()

        assertEquals(ConfirmationState.None, viewModel.uiState.value.confirmationState)
        coVerify(exactly = 0) { attendanceDao.insertRecord(any()) }
    }
}
