package com.example.presentmate.viewmodel

import app.cash.turbine.test
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuth
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns mockk(relaxed = true) {
                every { uid } returns "test_user"
            }
        }
        every { FirebaseAuth.getInstance() } returns mockAuth

        // Default mock responses
        every { attendanceDao.getOngoingSession(any()) } returns null
        every { attendanceDao.getOngoingSessionFlow(any()) } returns flowOf(null)
        every { attendanceDao.getAllRecords(any()) } returns flowOf(emptyList())
        coEvery { attendanceDao.insertRecord(any()) } returns Unit
        coEvery { attendanceDao.updateRecord(any()) } returns Unit

        viewModel = AttendanceViewModel(attendanceDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun startSession_insertsRecord() = runTest {
        viewModel.startSession()
        advanceUntilIdle()

        coVerify {
            attendanceDao.insertRecord(withArg { record ->
                assertTrue(record.timeIn != null)
                assertTrue(record.timeOut == null)
                assertEquals("test_user", record.userId)
            })
        }
    }

    @Test
    fun endSession_updatesOngoingSession() = runTest {
        val ongoingRecord = AttendanceRecord(
            id = 1,
            userId = "test_user",
            date = System.currentTimeMillis(),
            timeIn = System.currentTimeMillis(),
            timeOut = null
        )
        every { attendanceDao.getOngoingSession(any()) } returns ongoingRecord
        every { attendanceDao.getOngoingSessionFlow(any()) } returns flowOf(ongoingRecord)

        // Recreate viewModel so it picks up the new flow
        viewModel = AttendanceViewModel(attendanceDao)
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()

        coVerify {
            attendanceDao.updateRecord(withArg { record ->
                assertEquals(1, record.id)
                assertNotNull(record.timeOut)
            })
        }
    }

    @Test
    fun flowsEmittedCorrectly() = runTest {
        val testRecords = listOf(
            AttendanceRecord(id = 1, userId = "test_user", date = 1000L, timeIn = 2000L, timeOut = 3000L)
        )
        val ongoingRecord = AttendanceRecord(id = 2, userId = "test_user", date = 4000L, timeIn = 5000L, timeOut = null)

        every { attendanceDao.getAllRecords(any()) } returns flowOf(testRecords)
        every { attendanceDao.getOngoingSession(any()) } returns ongoingRecord
        every { attendanceDao.getOngoingSessionFlow(any()) } returns flowOf(ongoingRecord)

        viewModel = AttendanceViewModel(attendanceDao)

        // Use Turbine to properly collect StateFlow emissions
        viewModel.allRecords.test {
            val emitted = awaitItem()
            assertEquals(testRecords, emitted)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.ongoingSession.test {
            val emitted = awaitItem()
            assertEquals(ongoingRecord, emitted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateSessionTimeIn_updatesOngoingRecord() = runTest {
        val ongoingRecord = AttendanceRecord(
            id = 1,
            userId = "test_user",
            date = System.currentTimeMillis(),
            timeIn = 1000L,
            timeOut = null
        )
        every { attendanceDao.getOngoingSessionFlow(any()) } returns flowOf(ongoingRecord)
        every { attendanceDao.getOngoingSession(any()) } returns ongoingRecord

        viewModel = AttendanceViewModel(attendanceDao)
        advanceUntilIdle()

        val newTimeIn = 2000L
        viewModel.updateSessionTimeIn(newTimeIn)
        advanceUntilIdle()

        coVerify {
            attendanceDao.updateRecord(withArg { record ->
                assertEquals(newTimeIn, record.timeIn)
                assertEquals(1, record.id)
            })
        }
    }

    @Test
    fun endSession_whenNoOngoingSession_doesNothing() = runTest {
        // Default setUp has getOngoingSessionFlow returning flowOf(null)
        viewModel = AttendanceViewModel(attendanceDao)
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()

        // No update should be called since there is no ongoing session
        coVerify(exactly = 0) { attendanceDao.updateRecord(any()) }
    }
}
