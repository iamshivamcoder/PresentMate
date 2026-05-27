package com.example.presentmate.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StepActivityLog): Long

    @Query("SELECT * FROM step_activity_logs ORDER BY detectedAt DESC")
    fun getAllFlow(): Flow<List<StepActivityLog>>

    @Query("SELECT * FROM step_activity_logs ORDER BY detectedAt DESC")
    suspend fun getAll(): List<StepActivityLog>

    @Query("SELECT * FROM step_activity_logs WHERE detectedAt >= :since ORDER BY detectedAt DESC")
    suspend fun getSince(since: Long): List<StepActivityLog>

    @Query("SELECT * FROM step_activity_logs ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<StepActivityLog>

    @Query("DELETE FROM step_activity_logs WHERE detectedAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Delete
    suspend fun delete(log: StepActivityLog)

    @Query("DELETE FROM step_activity_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM step_activity_logs WHERE triggered = 1")
    suspend fun triggeredCount(): Int
}
