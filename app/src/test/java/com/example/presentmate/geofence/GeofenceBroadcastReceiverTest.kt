package com.example.presentmate.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.di.GeofenceBroadcastReceiverEntryPoint
import com.example.presentmate.di.getEntryPoint
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeofenceBroadcastReceiverTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    @MockK(relaxed = true)
    private lateinit var geofencingEvent: GeofencingEvent

    private lateinit var receiver: GeofenceBroadcastReceiver
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        receiver = GeofenceBroadcastReceiver()

        val entryPoint = mockk<GeofenceBroadcastReceiverEntryPoint>()
        every { entryPoint.attendanceDao() } returns attendanceDao
        every { entryPoint.applicationScope() } returns CoroutineScope(testDispatcher)

        mockkStatic(::getEntryPoint)
        every { getEntryPoint(any()) } returns entryPoint

        mockkStatic(GeofencingEvent::class)
        every { GeofencingEvent.fromIntent(any()) } returns geofencingEvent

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Mock GeofenceNotificationUtils to prevent null pointer exceptions
        mockkObject(GeofenceNotificationUtils)
        every { GeofenceNotificationUtils.showGeofenceErrorNotification(any(), any()) } just runs
        every { GeofenceNotificationUtils.showGeofenceEnterNotification(any(), any()) } just runs
        every { GeofenceNotificationUtils.showGeofenceExitNotification(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onReceive with enter transition should start a new session`() = runTest(testDispatcher) {
        every { geofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { geofencingEvent.hasError() } returns false
        coEvery { attendanceDao.getOngoingSessionFlow() } returns flowOf(null)
        coEvery { attendanceDao.insertRecord(any()) } just runs

        val intent = mockk<Intent>()

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            attendanceDao.insertRecord(any<AttendanceRecord>())
        }
    }

    @Test
    fun `onReceive with exit transition should end an ongoing session`() = runTest(testDispatcher) {
        val ongoingSession = AttendanceRecord(id = 1, date = 1L, timeIn = 1L, timeOut = null)
        every { geofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_EXIT
        every { geofencingEvent.hasError() } returns false
        coEvery { attendanceDao.getOngoingSessionFlow() } returns flowOf(ongoingSession)
        coEvery { attendanceDao.updateRecord(any()) } just runs

        val intent = mockk<Intent>()

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            attendanceDao.updateRecord(any<AttendanceRecord>())
        }
    }

    @Test
    fun `onReceive with enter transition when session exists should not create new session`() =
        runTest(testDispatcher) {
            val existingSession =
                AttendanceRecord(id = 1, date = 100L, timeIn = 100L, timeOut = null)
            every { geofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
            every { geofencingEvent.hasError() } returns false
            coEvery { attendanceDao.getOngoingSessionFlow() } returns flowOf(existingSession)

            val intent = mockk<Intent>()

            receiver.onReceive(context, intent)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                attendanceDao.insertRecord(any<AttendanceRecord>())
            }
        }

    @Test
    fun `onReceive with exit transition when no session exists should not update`() =
        runTest(testDispatcher) {
            every { geofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_EXIT
            every { geofencingEvent.hasError() } returns false
            coEvery { attendanceDao.getOngoingSessionFlow() } returns flowOf(null)

            val intent = mockk<Intent>()

            receiver.onReceive(context, intent)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                attendanceDao.updateRecord(any<AttendanceRecord>())
            }
        }

    @Test
    fun `onReceive with geofencing error should not process event`() = runTest(testDispatcher) {
        every { geofencingEvent.hasError() } returns true
        every { geofencingEvent.errorCode } returns 1000

        val intent = mockk<Intent>()

        receiver.onReceive(context, intent)
        advanceUntilIdle()

        coVerify(exactly = 0) {
            attendanceDao.insertRecord(any())
            attendanceDao.updateRecord(any())
        }
    }
}