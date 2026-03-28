package com.example.presentmate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.presentmate.ui.theme.PresentMateTheme
import com.example.presentmate.worker.SessionReminderNotificationUtils
import com.example.presentmate.worker.SessionReminderScheduler
import com.example.presentmate.worker.StepWindowScheduler
import com.example.presentmate.worker.StudyCheckpointNotificationUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "presentmate_permissions"
        private const val KEY_PERMISSIONS_REQUESTED = "permissions_requested_v1"

        // Shared UI state: set here, read by AppNavigation composable
        val recapLogId      = mutableStateOf<Int?>(null)   // null = no dialog
        val isRecapPartial  = mutableStateOf(false)
        val showReminderDialog = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep-link intents from notifications
        handleNotificationIntent(intent)

        // Request permissions only once
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PERMISSIONS_REQUESTED, false)) {
            requestNotificationPermission()
            requestLocationPermissions()
            requestActivityRecognitionPermission()
            prefs.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply()
        }

        // Ensure alarms are always scheduled
        SessionReminderScheduler.ensureScheduled(this)
        StepWindowScheduler.ensureScheduled(this)

        setContent {
            PresentMateTheme {
                AppNavigation()
            }
        }
    }

    /** Called when the activity is re-used by FLAG_ACTIVITY_SINGLE_TOP (notification tap). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        when (intent?.action) {
            StudyCheckpointNotificationUtils.ACTION_OPEN_RECAP -> {
                val logId = intent.getIntExtra(StudyCheckpointNotificationUtils.EXTRA_LOG_ID, -1)
                val isPartial = intent.getBooleanExtra("is_partial", false)
                if (logId != -1) {
                    Log.d("MainActivity", "Opening recap dialog for logId=$logId partial=$isPartial")
                    recapLogId.value = logId
                    isRecapPartial.value = isPartial
                }
            }
            SessionReminderNotificationUtils.ACTION_OPEN_REMINDER_DIALOG -> {
                Log.d("MainActivity", "Opening session reminder dialog")
                showReminderDialog.value = true
            }
            else -> {
                // Legacy: handle old partial_log_id extras
                val legacyLogId = intent?.getIntExtra("partial_log_id", -1) ?: -1
                if (legacyLogId != -1) {
                    recapLogId.value = legacyLogId
                    isRecapPartial.value = true
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 1002)
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1003
                )
            }
        }
    }
}
