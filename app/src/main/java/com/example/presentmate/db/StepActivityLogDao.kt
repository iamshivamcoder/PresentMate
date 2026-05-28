package com.example.presentmate.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StepActivityLog): Long

    @Query("SELECT * FROM step_activity_logs WHERE userId = :userId ORDER BY detectedAt DESC")
    fun getAllFlow(userId: String): Flow<List<StepActivityLog>>

    @Query("SELECT * FROM step_activity_logs WHERE userId = :userId ORDER BY detectedAt DESC")
    suspend fun getAll(userId: String): List<StepActivityLog>

    @Query("SELECT * FROM step_activity_logs WHERE detectedAt >= :since AND userId = :userId ORDER BY detectedAt DESC")
    suspend fun getSince(since: Long, userId: String): List<StepActivityLog>

    @Query("SELECT * FROM step_activity_logs WHERE userId = :userId ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50, userId: String): List<StepActivityLog>

    @Query("DELETE FROM step_activity_logs WHERE detectedAt < :before AND userId = :userId")
    suspend fun deleteOlderThan(before: Long, userId: String)

    @Delete
    suspend fun delete(log: StepActivityLog)

    @Query("DELETE FROM step_activity_logs WHERE userId = :userId")
    suspend fun clearAll(userId: String)

    @Query("SELECT COUNT(*) FROM step_activity_logs WHERE triggered = 1 AND userId = :userId")
    suspend fun triggeredCount(userId: String): Int
}
