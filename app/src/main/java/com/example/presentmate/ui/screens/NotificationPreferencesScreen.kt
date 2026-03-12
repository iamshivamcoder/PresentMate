package com.example.presentmate.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.presentmate.worker.SessionReminderScheduler

@Composable
fun NotificationPreferencesScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("session_reminder_prefs", Context.MODE_PRIVATE) }

    var savedHour by remember { mutableIntStateOf(prefs.getInt("reminder_hour", 9)) }
    var savedMinute by remember { mutableIntStateOf(prefs.getInt("reminder_minute", 30)) }
    var reminderEnabled by remember {
        mutableStateOf(prefs.getBoolean("reminder_enabled", true))
    }

    val hasNotificationPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Check exact alarm permission
    val canScheduleExact = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else true
    }

    val timeDisplayString = remember(savedHour, savedMinute) {
        val suffix = if (savedHour < 12) "AM" else "PM"
        val displayHour = when {
            savedHour == 0 -> 12
            savedHour > 12 -> savedHour - 12
            else -> savedHour
        }
        "$displayHour:${savedMinute.toString().padStart(2, '0')} $suffix"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (hasNotificationPermission)
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (hasNotificationPermission)
                        Icons.Default.CheckCircle else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (hasNotificationPermission) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasNotificationPermission) "Notifications Allowed ✅"
                        else "Notifications Not Allowed ❌",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasNotificationPermission) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (hasNotificationPermission)
                            "You'll receive daily session reminders"
                        else
                            "Grant permission to receive reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Enable/Disable Toggle
        SettingsGroup("Session Reminder") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Reminder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Mon–Sat at $timeDisplayString",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            reminderEnabled = enabled
                            prefs.edit().putBoolean("reminder_enabled", enabled).apply()
                            if (enabled) {
                                SessionReminderScheduler.scheduleNext(context)
                                Toast.makeText(context, "Reminder enabled", Toast.LENGTH_SHORT).show()
                            } else {
                                SessionReminderScheduler.cancel(context)
                                Toast.makeText(context, "Reminder disabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Time Picker
        SettingsGroup("Reminder Time") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Time", style = MaterialTheme.typography.titleMedium)
                            Text(
                                timeDisplayString,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    savedHour = hour
                                    savedMinute = minute
                                    prefs.edit()
                                        .putInt("reminder_hour", hour)
                                        .putInt("reminder_minute", minute)
                                        .apply()
                                    if (reminderEnabled) {
                                        SessionReminderScheduler.scheduleNext(context)
                                    }
                                    Toast.makeText(
                                        context,
                                        "Reminder time updated to $timeDisplayString",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                savedHour,
                                savedMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Change Time")
                    }
                }
            }
        }

        // Troubleshoot Section
        SettingsGroup("Troubleshoot") {
            FilledTonalButton(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Open Notification Settings")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Exact Alarm Permission")
                }
            }
        }
    }
}
