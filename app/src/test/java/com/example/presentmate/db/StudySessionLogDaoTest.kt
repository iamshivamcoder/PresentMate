package com.example.presentmate.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudySessionLogDaoTest {

    private lateinit var database: PresentMateDatabase
    private lateinit var dao: StudySessionLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            PresentMateDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.studySessionLogDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val log = StudySessionLog(
            userId = "test_user",
            calendarEventId = 1L,
            eventTitle = "Test Event",
            subject = "Math",
            topic = "Algebra",
            scheduledStartTime = 1000L,
            scheduledEndTime = 2000L
        )

        dao.insert(log)
        val insertedLog = dao.getByEventId(1L, "test_user")
        assertNotNull(insertedLog)

        val logById = dao.getById(insertedLog!!.id, "test_user")
        assertEquals("Test Event", logById?.eventTitle)
        assertEquals("test_user", logById?.userId)
    }

    @Test
    fun updateLog() = runBlocking {
        val log = StudySessionLog(
            userId = "test_user",
            calendarEventId = 1L,
            eventTitle = "Test Event",
            subject = null,
            topic = null,
            scheduledStartTime = 1000L,
            scheduledEndTime = 2000L
        )

        dao.insert(log)
        val insertedLog = dao.getByEventId(1L, "test_user")!!

        val updatedLog = insertedLog.copy(status = "COMPLETED", actualDurationMinutes = 60)
        dao.update(updatedLog)

        val fetchedLog = dao.getById(insertedLog.id, "test_user")
        assertEquals("COMPLETED", fetchedLog?.status)
        assertEquals(60, fetchedLog?.actualDurationMinutes)
    }

    @Test
    fun getPendingOverdue() = runBlocking {
        val log1 = StudySessionLog(
            userId = "test_user",
            calendarEventId = 1L, eventTitle = "Past Pending", subject = "Math", topic = "Algebra",
            scheduledStartTime = 1000L, scheduledEndTime = 2000L, status = "PENDING"
        )
        val log2 = StudySessionLog(
            userId = "test_user",
            calendarEventId = 2L, eventTitle = "Future Pending", subject = "Math", topic = "Algebra",
            scheduledStartTime = 5000L, scheduledEndTime = 6000L, status = "PENDING"
        )
        val log3 = StudySessionLog(
            userId = "test_user",
            calendarEventId = 3L, eventTitle = "Past Completed", subject = "Math", topic = "Algebra",
            scheduledStartTime = 1000L, scheduledEndTime = 2000L, status = "COMPLETED"
        )

        dao.insert(log1)
        dao.insert(log2)
        dao.insert(log3)

        // time is 3000 → log1 is overdue+pending, log2 not yet overdue, log3 overdue but not pending
        val overdueLogs = dao.getPendingOverdue(3000L, "test_user")

        assertEquals(1, overdueLogs.size)
        assertEquals("Past Pending", overdueLogs[0].eventTitle)
    }

    @Test
    fun log_cross_user_isolation() = runBlocking {
        val logA = StudySessionLog(
            userId = "user_a",
            calendarEventId = 99L,
            eventTitle = "User A Only",
            subject = null, topic = null,
            scheduledStartTime = 1000L, scheduledEndTime = 2000L
        )
        dao.insert(logA)

        // user_b should not see user_a's log
        val logForB = dao.getByEventId(99L, "user_b")
        assertNull(logForB)

        // user_a should see their own log
        val logForA = dao.getByEventId(99L, "user_a")
        assertNotNull(logForA)
        assertEquals("User A Only", logForA?.eventTitle)
    }
}
