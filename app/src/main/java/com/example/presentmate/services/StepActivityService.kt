package com.example.presentmate.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.presentmate.R
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StepActivityLog
import com.example.presentmate.worker.StepActivityNotificationUtils
import com.example.presentmate.worker.StepWindowScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Foreground service that listens for the Android Step Detector sensor during
 * two daily windows and fires a contextual notification if stair-climbing is detected.
 *
 * Detection heuristic:
 *   • Count steps via TYPE_STEP_DETECTOR over a rolling 30-second window
 *   • If ≥ STAIR_STEP_THRESHOLD steps are counted → trigger the appropriate notification
 *   • After firing, pause detection for COOLDOWN_MS to avoid repeated notifications
 *
 * Windows:
 *   • 9:00 – 10:00 AM  → "Heading to library?" (start session)
 *   • 8:00 – 9:00 PM   → "Heading home?"       (end session)
 */
class StepActivityService : Service(), SensorEventListener {

    // ── Config ─────────────────────────────────────────────────────────────
    private val STAIR_STEP_THRESHOLD = 15       // steps in WINDOW_MS to trigger
    private val WINDOW_MS            = 30_000L  // rolling window duration (30 sec)
    private val COOLDOWN_MS          = 10 * 60_000L // 10-min cool-down after firing
    private val FOREGROUND_NOTIF_ID  = 8001
    private val FOREGROUND_CHANNEL   = "step_service_channel"

    // ── State ──────────────────────────────────────────────────────────────
    private var sensorManager: SensorManager? = null
    private var stepDetector: Sensor? = null

    /** Timestamps of each detected step within the rolling window */
    private val stepTimestamps = ArrayDeque<Long>()
    private var lastFiredAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        stepDetector  = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        Log.d(TAG, "Service created. Step detector available: ${stepDetector != null}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification())

        stepDetector?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step detector registered for action: ${intent?.action}")
        } ?: run {
            Log.w(TAG, "No step detector sensor — stopping service")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        ioScope.cancel()  // prevent coroutine leaks after service is destroyed
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Shared prefs key for last known step count (read by StepSyncWorker) ──
    private val syncPrefs by lazy {
        getSharedPreferences("step_sync_prefs", MODE_PRIVATE)
    }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_DETECTOR) return

        val now = System.currentTimeMillis()

        // Persist cumulative step count for StepSyncWorker (fix #8)
        val currentTotal = syncPrefs.getLong("last_step_total", 0L) + 1
        syncPrefs.edit().putLong("last_step_total", currentTotal).apply()

        // Add this step and prune anything older than the rolling window
        stepTimestamps.addLast(now)
        while (stepTimestamps.isNotEmpty() && (now - stepTimestamps.first()) > WINDOW_MS) {
            stepTimestamps.removeFirst()
        }

        val windowCount = stepTimestamps.size
        Log.d(TAG, "Steps in ${WINDOW_MS / 1000}s window: $windowCount")

        if (windowCount >= STAIR_STEP_THRESHOLD && (now - lastFiredAt) > COOLDOWN_MS) {
            lastFiredAt = now
            stepTimestamps.clear() // reset window after firing

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val windowLabel = when {
                hour in 9..9   -> "MORNING"
                hour in 20..20 -> "EVENING"
                else           -> null
            }

            when (windowLabel) {
                "MORNING" -> {
                    Log.d(TAG, "Morning stair burst detected — showing 'going out' notification")
                    StepActivityNotificationUtils.showGoingOutNotification(this)
                }
                "EVENING" -> {
                    Log.d(TAG, "Evening stair burst detected — showing 'going home' notification")
                    StepActivityNotificationUtils.showGoingHomeNotification(this)
                }
                else -> {
                    Log.d(TAG, "Burst detected outside target window — ignoring")
                    return
                }
            }

            // Fix #9 — persist burst to history DB
            ioScope.launch {
                try {
                    val db = PresentMateDatabase.getDatabase(applicationContext)
                    db.stepActivityLogDao().insert(
                        StepActivityLog(
                            detectedAt    = now,
                            stepCount     = windowCount,
                            windowMinutes = (WINDOW_MS / 60_000).toInt(),
                            type          = "STAIR_BURST",
                            window        = windowLabel,
                            triggered     = true
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log burst event", e)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* unused */ }

    // ── Foreground notification ────────────────────────────────────────────

    private fun buildForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL,
                "Activity Monitor",
                NotificationManager.IMPORTANCE_MIN  // silent, no sound/vibrate
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("PresentMate is watching your steps 👣")
            .setContentText("Stair activity detection active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "StepActivityService"
    }
}
