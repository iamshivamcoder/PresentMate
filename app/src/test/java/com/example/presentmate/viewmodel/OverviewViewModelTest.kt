package com.example.presentmate.viewmodel

import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.GraphViewType
import com.example.presentmate.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    // A fixed reference date to keep tests deterministic
    private val today = LocalDate.of(2024, 3, 13)

    /** Converts a LocalDate to epoch-millis at midnight in the system timezone */
    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Mock FirebaseAuth so the ViewModel's internal call doesn't crash.
        // The ViewModel calls FirebaseAuth.getInstance() inside coroutines that may run on
        // background threads — we mock at the static level AND set up the DAO to respond
        // to any uid so even if the mock isn't applied the DAO won't block.
        mockkStatic(FirebaseAuth::class)
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "test_user"
        }
        val mockAuth = mockk<FirebaseAuth>(relaxed = true) {
            every { currentUser } returns mockUser
        }
        every { FirebaseAuth.getInstance() } returns mockAuth

        // Always return safe defaults for any userId string.
        // Using any() means these stubs apply regardless of what uid the ViewModel resolves.
        every { attendanceDao.getAllRecords(any()) } returns flowOf(emptyList())
        coEvery { attendanceDao.getAllRecordsNonFlow(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ------------------------------------------------------------------
    // Test 1: Complete records only — null timeOut excluded
    // ------------------------------------------------------------------
    @Test
    fun `records with null timeOut are excluded from daily summaries`() = runTest {
        val complete = AttendanceRecord(
            id = 1, userId = "test_user",
            date = today.toEpochMillis(),
            timeIn = today.toEpochMillis() + 1_000L,
            timeOut = today.toEpochMillis() + 3_601_000L  // 1 hour + 1s
        )
        val ongoing = AttendanceRecord(
            id = 2, userId = "test_user",
            date = today.toEpochMillis(),
            timeIn = today.toEpochMillis() + 4_000_000L,
            timeOut = null
        )

        every { attendanceDao.getAllRecords(any()) } returns flowOf(listOf(complete, ongoing))
        coEvery { attendanceDao.getAllRecordsNonFlow(any()) } returns listOf(complete, ongoing)

        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        assertEquals(1, summaries.size)
    }

    // ------------------------------------------------------------------
    // Test 2: Multiple complete records on same day are aggregated
    // ------------------------------------------------------------------
    @Test
    fun `multiple complete records on same day are summed into one daily summary`() = runTest {
        val s1 = today.toEpochMillis() + 9 * 3_600_000L   // 09:00
        val e1 = today.toEpochMillis() + 12 * 3_600_000L  // 12:00 (3h)
        val s2 = today.toEpochMillis() + 13 * 3_600_000L  // 13:00
        val e2 = today.toEpochMillis() + 17 * 3_600_000L  // 17:00 (4h)

        val records = listOf(
            AttendanceRecord(id = 1, userId = "test_user", date = today.toEpochMillis(), timeIn = s1, timeOut = e1),
            AttendanceRecord(id = 2, userId = "test_user", date = today.toEpochMillis(), timeIn = s2, timeOut = e2)
        )
        every { attendanceDao.getAllRecords(any()) } returns flowOf(records)
        coEvery { attendanceDao.getAllRecordsNonFlow(any()) } returns records

        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        assertEquals(1, summaries.size)
        assertEquals(7 * 3_600_000L, summaries[0].totalDurationMillis)
    }

    // ------------------------------------------------------------------
    // Test 3: Records on different days → sorted descending
    // ------------------------------------------------------------------
    @Test
    fun `records on different days produce separate daily summaries sorted descending`() = runTest {
        val yesterday = today.minusDays(1)
        val records = listOf(
            AttendanceRecord(id = 1, userId = "test_user", date = yesterday.toEpochMillis(),
                timeIn = yesterday.toEpochMillis() + 3_600_000L,
                timeOut = yesterday.toEpochMillis() + 7_200_000L),
            AttendanceRecord(id = 2, userId = "test_user", date = today.toEpochMillis(),
                timeIn = today.toEpochMillis() + 3_600_000L,
                timeOut = today.toEpochMillis() + 7_200_000L)
        )
        every { attendanceDao.getAllRecords(any()) } returns flowOf(records)
        coEvery { attendanceDao.getAllRecordsNonFlow(any()) } returns records

        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        val summaries = viewModel.uiState.value.dailySummaries
        assertEquals(2, summaries.size)
        assertTrue(summaries[0].date.isAfter(summaries[1].date))
    }

    // ------------------------------------------------------------------
    // Test 4: Empty records
    // ------------------------------------------------------------------
    @Test
    fun `empty records list results in empty daily summaries`() = runTest {
        // setUp already mocks empty list — just create viewModel
        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dailySummaries.isEmpty())
    }

    // ------------------------------------------------------------------
    // Test 5: onViewTypeChange
    // ------------------------------------------------------------------
    @Test
    fun `onViewTypeChange updates selectedGraphViewType in uiState`() = runTest {
        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        assertEquals(GraphViewType.WEEKLY, viewModel.uiState.value.selectedGraphViewType)

        viewModel.onViewTypeChange(GraphViewType.MONTHLY)
        advanceUntilIdle()

        assertEquals(GraphViewType.MONTHLY, viewModel.uiState.value.selectedGraphViewType)
    }

    // ------------------------------------------------------------------
    // Test 6: onDateChange
    // ------------------------------------------------------------------
    @Test
    fun `onDateChange updates currentDisplayDate in uiState`() = runTest {
        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        val newDate = LocalDate.of(2024, 1, 1)
        viewModel.onDateChange(newDate)
        advanceUntilIdle()

        assertEquals(newDate, viewModel.uiState.value.currentDisplayDate)
    }

    // ------------------------------------------------------------------
    // New Test: records with timeOut before timeIn are excluded
    // ------------------------------------------------------------------
    @Test
    fun `records with timeOut before timeIn are excluded from summaries`() = runTest {
        val invalidRecord = AttendanceRecord(
            id = 1, userId = "test_user",
            date = today.toEpochMillis(),
            timeIn = today.toEpochMillis() + 10_000L,
            timeOut = today.toEpochMillis() + 5_000L  // timeOut < timeIn = invalid
        )
        every { attendanceDao.getAllRecords(any()) } returns flowOf(listOf(invalidRecord))
        coEvery { attendanceDao.getAllRecordsNonFlow(any()) } returns listOf(invalidRecord)

        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dailySummaries.isEmpty())
    }

    // ------------------------------------------------------------------
    // New Test: view type toggles correctly
    // ------------------------------------------------------------------
    @Test
    fun `onViewTypeChange to monthly then back to weekly reflects in state`() = runTest {
        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        viewModel.onViewTypeChange(GraphViewType.MONTHLY)
        advanceUntilIdle()
        assertEquals(GraphViewType.MONTHLY, viewModel.uiState.value.selectedGraphViewType)

        viewModel.onViewTypeChange(GraphViewType.WEEKLY)
        advanceUntilIdle()
        assertEquals(GraphViewType.WEEKLY, viewModel.uiState.value.selectedGraphViewType)
    }

    // ------------------------------------------------------------------
    // New Test: stats are zero with no data
    // ------------------------------------------------------------------
    @Test
    fun `stats totalHours and bestDay are zero and dash when no records`() = runTest {
        val viewModel = OverviewViewModel(attendanceDao, mainDispatcherRule.testDispatcher)
        advanceUntilIdle()

        assertEquals(0f, viewModel.uiState.value.stats.totalHours, 0.001f)
        assertEquals("-", viewModel.uiState.value.stats.bestDay)
    }
}
