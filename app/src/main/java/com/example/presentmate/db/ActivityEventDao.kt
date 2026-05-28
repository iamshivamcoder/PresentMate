package com.example.presentmate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ActivityEvent>)

    // For Pagination using standard Room offsets (or Paging3 in the future)
    @Query("SELECT * FROM activity_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getEvents(userId: String, limit: Int, offset: Int): List<ActivityEvent>
    
    // For reactive updates of the most recent events
    @Query("SELECT * FROM activity_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(userId: String, limit: Int = 50): Flow<List<ActivityEvent>>

    @Query("DELETE FROM activity_events WHERE timestamp < :olderThan AND isSynced = 1")
    suspend fun cleanupOldSyncedEvents(olderThan: Long)
    
    @Query("SELECT * FROM activity_events WHERE isSynced = 0")
    suspend fun getUnsyncedEvents(): List<ActivityEvent>
    
    @Query("UPDATE activity_events SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
