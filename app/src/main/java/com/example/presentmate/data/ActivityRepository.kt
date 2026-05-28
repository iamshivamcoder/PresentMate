package com.example.presentmate.data

import com.example.presentmate.db.ActivityEvent
import com.example.presentmate.db.ActivityEventDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityEventDao: ActivityEventDao,
    private val authRepository: AuthRepository
) {
    suspend fun insertEvent(eventType: String, metadataJson: String? = null) {
        val user = authRepository.currentUser
        if (user == null) return // Or throw, depending on strictness
        
        val event = ActivityEvent(
            userId = user.uid,
            eventType = eventType,
            metadataJson = metadataJson
        )
        activityEventDao.insertEvent(event)
    }
    
    fun getRecentEventsFlow(limit: Int = 50): Flow<List<ActivityEvent>> {
        val uid = authRepository.currentUser?.uid ?: ""
        return activityEventDao.getRecentEventsFlow(uid, limit)
    }
    
    suspend fun cleanupOldEvents(olderThanMs: Long) {
        activityEventDao.cleanupOldSyncedEvents(olderThanMs)
    }
}
