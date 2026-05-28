package com.example.presentmate.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class EventLogger @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val applicationScope: CoroutineScope
) {
    /**
     * Non-suspending helper to fire-and-forget an activity event.
     * This makes it easy to add logging to any part of the app without
     * needing to be inside a suspend function or managing coroutines.
     */
    fun logEventAsync(eventType: String, metadataJson: String? = null) {
        applicationScope.launch {
            try {
                activityRepository.insertEvent(eventType, metadataJson)
            } catch (e: Exception) {
                Log.e("EventLogger", "Failed to log event: $eventType", e)
            }
        }
    }
    
    suspend fun logEvent(eventType: String, metadataJson: String? = null) {
        activityRepository.insertEvent(eventType, metadataJson)
    }
}
