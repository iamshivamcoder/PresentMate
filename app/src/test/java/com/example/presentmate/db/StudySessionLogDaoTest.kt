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
            calendarEventId = 1L,
            eventTitle = "Test Event",
            subject = "Math",
            topic = "Algebra",
            scheduledStartTime = 1000L,
            scheduledEndTime = 2000L
        )

        dao.insert(log)
        // Since id is auto-generated, we get it by event ID first to know the id
        val insertedLog = dao.getByEventId(1L)
        assertNotNull(insertedLog)
        
        val logById = dao.getById(insertedLog!!.id)
        assertEquals("Test Event", logById?.eventTitle)
    }

    @Test
    fun updateLog() = runBlocking {
        val log = StudySessionLog(
            calendarEventId = 1L,
            eventTitle = "Test Event",
            subject = null,
            topic = null,
            scheduledStartTime = 1000L,
            scheduledEndTime = 2000L
        )

        dao.insert(log)
        val insertedLog = dao.getByEventId(1L)!!
        
        val updatedLog = insertedLog.copy(status = "COMPLETED", actualDurationMinutes = 60)
        dao.update(updatedLog)

        val fetchedLog = dao.getById(insertedLog.id)
        assertEquals("COMPLETED", fetchedLog?.status)
        assertEquals(60, fetchedLog?.actualDurationMinutes)
    }

    @Test
    fun getPendingOverdue() = runBlocking {
        val log1 = StudySessionLog(
            calendarEventId = 1L, eventTitle = "Past Pending", subject = "Math", topic = "Algebra",
            scheduledStartTime = 1000L, scheduledEndTime = 2000L, status = "PENDING"
        )
        val log2 = StudySessionLog(
            calendarEventId = 2L, eventTitle = "Future Pending", subject = "Math", topic = "Algebra",
            scheduledStartTime = 5000L, scheduledEndTime = 6000L, status = "PENDING"
        )
        val log3 = StudySessionLog(
            calendarEventId = 3L, eventTitle = "Past Completed", subject = "Math", topic = "Algebra",
            scheduledStartTime = 1000L, scheduledEndTime = 2000L, status = "COMPLETED"
        )

        dao.insert(log1)
        dao.insert(log2)
        dao.insert(log3)

        // time is 3000, so log1 is overdue, log2 is not overdue, log3 is overdue but not pending
        val overdueLogs = dao.getPendingOverdue(3000L)
        
        assertEquals(1, overdueLogs.size)
        assertEquals("Past Pending", overdueLogs[0].eventTitle)
    }
}
