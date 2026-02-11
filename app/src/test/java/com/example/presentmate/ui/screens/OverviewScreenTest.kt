package com.example.presentmate.ui.screens

import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.GraphViewType
import com.example.presentmate.viewmodel.OverviewUiState
import com.example.presentmate.viewmodel.OverviewViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewScreenTest {

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    private lateinit var viewModel: OverviewViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state should have empty daily summaries`() = runTest(testDispatcher) {
        // Given
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        coEvery { attendanceDao.getAllRecordsNonFlow() } returns emptyList()

        // When
        viewModel = OverviewViewModel(attendanceDao)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.dailySummaries.isEmpty())
    }

    @Test
    fun `initial state should have weekly graph view type`() = runTest(testDispatcher) {
        // Given
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        coEvery { attendanceDao.getAllRecordsNonFlow() } returns emptyList()

        // When
        viewModel = OverviewViewModel(attendanceDao)
        advanceUntilIdle()

        // Then
        assertEquals(GraphViewType.WEEKLY, viewModel.uiState.value.selectedGraphViewType)
    }

    @Test
    fun `onViewTypeChange should update graph view type`() = runTest(testDispatcher) {
        // Given
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        coEvery { attendanceDao.getAllRecordsNonFlow() } returns emptyList()
        viewModel = OverviewViewModel(attendanceDao)
        advanceUntilIdle()

        // When
        viewModel.onViewTypeChange(GraphViewType.MONTHLY)
        advanceUntilIdle()

        // Then
        assertEquals(GraphViewType.MONTHLY, viewModel.uiState.value.selectedGraphViewType)
    }

    @Test
    fun `onDateChange should update current display date`() = runTest(testDispatcher) {
        // Given
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        coEvery { attendanceDao.getAllRecordsNonFlow() } returns emptyList()
        viewModel = OverviewViewModel(attendanceDao)
        advanceUntilIdle()

        val newDate = LocalDate.of(2024, 6, 15)

        // When
        viewModel.onDateChange(newDate)
        advanceUntilIdle()

        // Then
        assertEquals(newDate, viewModel.uiState.value.currentDisplayDate)
    }
}
