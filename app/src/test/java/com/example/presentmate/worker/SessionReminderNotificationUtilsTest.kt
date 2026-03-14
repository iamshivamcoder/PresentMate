package com.example.presentmate.worker

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionReminderNotificationUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCreateNotificationChannel() {
        // Attempt to create the channel
        SessionReminderNotificationUtils.createNotificationChannel(context)

        // Verify that the channel was created
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(SessionReminderNotificationUtils.CHANNEL_ID)
        
        assertNotNull("Notification channel should be created", channel)
        assertEquals("Session Reminders", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }
}
