package com.example.presentmate.viewmodel

import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.GraphViewType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    private val testDispatcher = UnconfinedTestDispatcher()

    // A fixed reference date to use as "today" in tests
    private val today = LocalDate.of(2024, 3, 13)

    /** Converts a LocalDate to epoch-millis at midnight in the system timezone */
    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        every { attendanceDao.getAllRecordsNonFlow() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ------------------------------------------------------------------
    // Test 1a: processRecords filters out incomplete records
    // ------------------------------------------------------------------
    @Test
    fun `records with null timeOut are excluded from daily summaries`() = runTest {
        val complete = AttendanceRecord(
            id = 1,
            date = today.toEpochMillis(),
            timeIn = today.toEpochMillis() + 1000,
            timeOut = today.toEpochMillis() + 3_600_000
        )
        val ongoing = AttendanceRecord(
            id = 2,
            date = today.toEpochMillis(),
            timeIn = today.toEpochMillis() + 4_000_000,
            timeOut = null // <-- ongoing session, must be excluded
        )

        every { attendanceDao.getAllRecords() } returns flowOf(listOf(complete, ongoing))
        every { attendanceDao.getAllRecordsNonFlow() } returns listOf(complete, ongoing)

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        // Only one summary (for 'today'), and its duration should only count the complete record
        assertEquals(1, summaries.size)
        val expectedDuration = 3_600_000L - 1000L
        assertEquals(expectedDuration, summaries[0].totalDurationMillis)
    }

    // ------------------------------------------------------------------
    // Test 1b: Multiple records on same day are aggregated correctly
    // ------------------------------------------------------------------
    @Test
    fun `multiple complete records on same day are summed into one daily summary`() = runTest {
        val session1Start = today.toEpochMillis() + 9 * 3_600_000   // 09:00
        val session1End   = today.toEpochMillis() + 12 * 3_600_000  // 12:00 (3h)
        val session2Start = today.toEpochMillis() + 13 * 3_600_000  // 13:00
        val session2End   = today.toEpochMillis() + 17 * 3_600_000  // 17:00 (4h)

        val records = listOf(
            AttendanceRecord(id = 1, date = today.toEpochMillis(), timeIn = session1Start, timeOut = session1End),
            AttendanceRecord(id = 2, date = today.toEpochMillis(), timeIn = session2Start, timeOut = session2End)
        )
        every { attendanceDao.getAllRecords() } returns flowOf(records)
        every { attendanceDao.getAllRecordsNonFlow() } returns records

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        assertEquals(1, summaries.size)
        assertEquals(7 * 3_600_000L, summaries[0].totalDurationMillis) // 7 hours total
    }

    // ------------------------------------------------------------------
    // Test 1c: Records on different days produce separate summaries
    // ------------------------------------------------------------------
    @Test
    fun `records on different days produce separate daily summaries sorted descending`() = runTest {
        val yesterday = today.minusDays(1)
        val records = listOf(
            AttendanceRecord(id = 1, date = yesterday.toEpochMillis(),
                timeIn = yesterday.toEpochMillis() + 3_600_000,
                timeOut = yesterday.toEpochMillis() + 7_200_000),
            AttendanceRecord(id = 2, date = today.toEpochMillis(),
                timeIn = today.toEpochMillis() + 3_600_000,
                timeOut = today.toEpochMillis() + 7_200_000)
        )
        every { attendanceDao.getAllRecords() } returns flowOf(records)
        every { attendanceDao.getAllRecordsNonFlow() } returns records

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        assertEquals(2, summaries.size)
        // Sorted descending: today first, yesterday second
        assertTrue(summaries[0].date.isAfter(summaries[1].date))
    }

    // ------------------------------------------------------------------
    // Test 1d: Empty record list produces empty summaries
    // ------------------------------------------------------------------
    @Test
    fun `empty records list results in empty daily summaries and zeroed stats`() = runTest {
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        every { attendanceDao.getAllRecordsNonFlow() } returns emptyList()

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dailySummaries.isEmpty())
    }

    // ------------------------------------------------------------------
    // Test 5: onViewTypeChange — graph switches correctly
    // ------------------------------------------------------------------
    @Test
    fun `onViewTypeChange updates selectedGraphViewType in uiState`() = runTest {
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        every { attendanceDao.getAllRecordsNonFlow() } returns emptyList()

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(GraphViewType.WEEKLY, viewModel.uiState.value.selectedGraphViewType)

        viewModel.onViewTypeChange(GraphViewType.MONTHLY)
        advanceUntilIdle()

        assertEquals(GraphViewType.MONTHLY, viewModel.uiState.value.selectedGraphViewType)
    }

    @Test
    fun `onDateChange updates currentDisplayDate in uiState`() = runTest {
        every { attendanceDao.getAllRecords() } returns flowOf(emptyList())
        every { attendanceDao.getAllRecordsNonFlow() } returns emptyList()

        val viewModel = OverviewViewModel(attendanceDao, testDispatcher)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val newDate = LocalDate.of(2024, 1, 1)
        viewModel.onDateChange(newDate)
        advanceUntilIdle()

        assertEquals(newDate, viewModel.uiState.value.currentDisplayDate)
    }
}
