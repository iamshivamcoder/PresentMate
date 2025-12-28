package com.example.presentmate.ui.screens

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.presentmate.db.AttendanceLogList
import com.example.presentmate.ui.components.MotivationalAnimation
import com.example.presentmate.viewmodel.AttendanceViewModel
import com.example.presentmate.data.GeofencePreferencesRepository
import com.example.presentmate.utils.DateTimeFormatters
import com.example.presentmate.utils.LocationUtils
import java.util.Date

@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var recordedTimeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    val ongoingSession by viewModel.ongoingSession.collectAsState()
    val attendanceRecords by viewModel.allRecords.collectAsState()
    val sessionInProgress = ongoingSession != null

    // Check if location services are enabled
    val isLocationEnabled = LocationUtils.isLocationEnabled(context)

    // Check if geofence tracking is enabled
    val isGeofenceTrackingEnabled = GeofencePreferencesRepository.isGeofenceEnabled(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sessionInProgress) {
            Spacer(modifier = Modifier.weight(0.1f))
            // Motivation box at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MotivationalAnimation()
                }
            }

            // Geofence status indicator
            if (isGeofenceTrackingEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocationEnabled) {
                            Color(0xFF4CAF50).copy(alpha = 0.3f) // Green for active geofence
                        } else {
                            Color(0xFFFF9800).copy(alpha = 0.3f) // Orange for disabled location
                        }
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
                            imageVector = if (isLocationEnabled) Icons.Default.LocationOn else Icons.Default.LocationDisabled,
                            contentDescription = null,
                            tint = if (isLocationEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isLocationEnabled) "Geofence Active" else "Location Disabled",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isLocationEnabled) Color(0xFF4CAF50) else Color(
                                    0xFFFF9800
                                )
                            )
                            Text(
                                text = if (isLocationEnabled) "Automatic session tracking is enabled" else "Enable location services for geofence tracking",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.weight(0.1f))
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val timeInButtonModifier = Modifier
                .weight(1f)
                .height(56.dp)
            val timeOutButtonModifier = Modifier
                .weight(1f)
                .height(56.dp)

            if (sessionInProgress) {
                // Session is in progress: Time In is Outlined and Disabled, Time Out is Filled and Enabled
                OutlinedButton(
                    onClick = { /* Time In is disabled when session is in progress */ },
                    enabled = false, // Corrected: Time In button is disabled when session is in progress
                    modifier = timeInButtonModifier,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Time In", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = {
                        val currentTime = DateTimeFormatters.formatTime(System.currentTimeMillis())
                        dialogMessage = "End session at $currentTime?"
                        recordedTimeAction = {
                            viewModel.endSession()
                        }
                        showDialog = true
                    },
                    enabled = true, // Corrected: Time Out button is enabled when session is in progress
                    modifier = timeOutButtonModifier,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Time Out", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // No session in progress: Time In is Filled and Enabled, Time Out is Outlined and Disabled
                Button(
                    onClick = {
                        val currentTime = DateTimeFormatters.formatTime(System.currentTimeMillis())
                        dialogMessage = "Start session at $currentTime?"
                        recordedTimeAction = {
                            viewModel.startSession()
                        }
                        showDialog = true
                    },
                    enabled = true, // Corrected: Time In button is enabled when no session is in progress
                    modifier = timeInButtonModifier,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Start Session", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = { /* Time Out is disabled when no session is in progress */ },
                    enabled = false, // Corrected: Time Out button is disabled when no session is in progress
                    modifier = timeOutButtonModifier,
                    shape = RoundedCornerShape(16.dp)
                ) {
                        Text("End Session", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (sessionInProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Session in progress...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Attendance Log",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AttendanceLogList(records = attendanceRecords)
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Confirm Action") },
            text = { Text(text = dialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordedTimeAction?.invoke()
                        showDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}
