package com.example.presentmate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.presentmate.ui.theme.PresentMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val PREFS_NAME = "presentmate_permissions"
        private const val KEY_PERMISSIONS_REQUESTED = "permissions_requested_v1"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle partial_log_id intent from notification
        val partialLogId = intent.getIntExtra("partial_log_id", -1)
        if (partialLogId != -1) {
            Log.d("MainActivity", "Received partial_log_id: $partialLogId")
            // TODO: Navigate to partial duration input dialog/screen
            // For now, log it for future implementation
        }

        // Request permissions only once
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasRequestedPerms = prefs.getBoolean(KEY_PERMISSIONS_REQUESTED, false)
        
        if (!hasRequestedPerms) {
            requestNotificationPermission()
            requestLocationPermissions()
            prefs.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply()
        }

        // Ensure daily session reminder alarm is always scheduled
        com.example.presentmate.worker.SessionReminderScheduler.ensureScheduled(this)

        setContent {
            PresentMateTheme {
                AppNavigation()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // For Android 10+ (API 29+), we also need background location permission for geofencing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Check if we have all the permissions
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        // Request missing permissions
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                1002 // Different request code for location permissions
            )
        }
    }
}
