package com.example.presentmate.viewmodel

import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    private lateinit var viewModel: AttendanceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Default mock responses
        every { attendanceDao.getOngoingSession() } returns flowOf(null)
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        coEvery { attendanceDao.insertRecord(any()) } returns Unit
        coEvery { attendanceDao.updateRecord(any()) } returns Unit

        viewModel = AttendanceViewModel(attendanceDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            })
        }
    }

    @Test
    fun endSession_updatesOngoingSession() = runTest {
        val ongoingRecord = AttendanceRecord(id = 1, date = System.currentTimeMillis(), timeIn = System.currentTimeMillis(), timeOut = null)
        every { attendanceDao.getOngoingSession() } returns flowOf(ongoingRecord)
        
        // Recreate viewModel so it picks up the flow
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
            AttendanceRecord(id = 1, date = 1000L, timeIn = 2000L, timeOut = 3000L)
        )
        val ongoingRecord = AttendanceRecord(id = 2, date = 4000L, timeIn = 5000L, timeOut = null)

        every { attendanceDao.getAllRecords() } returns flowOf(testRecords)
        every { attendanceDao.getOngoingSession() } returns flowOf(ongoingRecord)

        viewModel = AttendanceViewModel(attendanceDao)
        advanceUntilIdle()

        assertEquals(testRecords, viewModel.allRecords.value)
        assertEquals(ongoingRecord, viewModel.ongoingSession.value)
    }
}
