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

    @Query("SELECT * FROM study_session_logs WHERE calendarEventId = :eventId LIMIT 1")
    suspend fun getByEventId(eventId: Long): StudySessionLog?

    @Query("SELECT * FROM study_session_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): StudySessionLog?

    @Query("SELECT * FROM study_session_logs WHERE status = 'PENDING' AND scheduledEndTime < :now")
    suspend fun getPendingOverdue(now: Long): List<StudySessionLog>

    @Query("SELECT * FROM study_session_logs ORDER BY scheduledStartTime DESC")
    fun getAllLogs(): Flow<List<StudySessionLog>>

    @Query("SELECT * FROM study_session_logs WHERE scheduledStartTime >= :dayStart AND scheduledStartTime < :dayEnd ORDER BY scheduledStartTime ASC")
    fun getLogsForDay(dayStart: Long, dayEnd: Long): Flow<List<StudySessionLog>>
}
