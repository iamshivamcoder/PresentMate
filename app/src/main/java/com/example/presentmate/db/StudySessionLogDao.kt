package com.example.presentmate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StudySessionLog)

    @Update
    suspend fun update(log: StudySessionLog)

    @Query("SELECT * FROM study_session_logs WHERE calendarEventId = :eventId AND userId = :userId LIMIT 1")
    suspend fun getByEventId(eventId: Long, userId: String): StudySessionLog?

    @Query("SELECT * FROM study_session_logs WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: Int, userId: String): StudySessionLog?

    @Query("SELECT * FROM study_session_logs WHERE status = 'PENDING' AND scheduledEndTime < :now AND userId = :userId")
    suspend fun getPendingOverdue(now: Long, userId: String): List<StudySessionLog>

    /** Fix #16 — returns all PENDING logs so CalendarSyncWorker can cross-check against live events. */
    @Query("SELECT * FROM study_session_logs WHERE status = 'PENDING' AND userId = :userId")
    suspend fun getPendingLogs(userId: String): List<StudySessionLog>

    /** Fix #16 — deletes the log for a cancelled/removed calendar event. */
    @Query("DELETE FROM study_session_logs WHERE calendarEventId = :eventId AND userId = :userId")
    suspend fun deleteByEventId(eventId: Long, userId: String)

    @Query("SELECT * FROM study_session_logs WHERE userId = :userId ORDER BY scheduledStartTime DESC")
    fun getAllLogs(userId: String): Flow<List<StudySessionLog>>

    @Query("SELECT * FROM study_session_logs WHERE userId = :userId ORDER BY scheduledStartTime DESC")
    suspend fun getAllNonFlow(userId: String): List<StudySessionLog>

    @Query("SELECT * FROM study_session_logs WHERE scheduledStartTime >= :dayStart AND scheduledStartTime < :dayEnd AND userId = :userId ORDER BY scheduledStartTime ASC")
    fun getLogsForDay(dayStart: Long, dayEnd: Long, userId: String): Flow<List<StudySessionLog>>
}
