package com.example.presentmate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.navigation.NavHostController
import com.example.presentmate.db.AttendanceLogList
import com.example.presentmate.ui.components.MotivationalAnimation
import com.example.presentmate.viewmodel.AttendanceViewModel
import com.example.presentmate.data.GeofencePreferencesRepository
import com.example.presentmate.utils.DateTimeFormatters
import com.example.presentmate.utils.LocationUtils

@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var recordedTimeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    val ongoingSession by viewModel.ongoingSession.collectAsState()
    val attendanceRecords by viewModel.allRecords.collectAsState()
    val sessionInProgress = ongoingSession != null

    val isLocationEnabled = LocationUtils.isLocationEnabled(context)
    val isGeofenceTrackingEnabled = GeofencePreferencesRepository.isGeofenceEnabled(context)

    val listState = rememberLazyListState()
    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 50 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Motivational card when session is running
            if (sessionInProgress) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                }

                // Geofence status
                if (isGeofenceTrackingEnabled) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLocationEnabled)
                                    Color(0xFF4CAF50).copy(alpha = 0.3f)
                                else
                                    Color(0xFFFF9800).copy(alpha = 0.3f)
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
                                    imageVector = if (isLocationEnabled) Icons.Default.LocationOn
                                    else Icons.Default.LocationDisabled,
                                    contentDescription = null,
                                    tint = if (isLocationEnabled) Color(0xFF4CAF50)
                                    else Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isLocationEnabled) "Geofence Active"
                                        else "Location Disabled",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isLocationEnabled) Color(0xFF4CAF50)
                                        else Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = if (isLocationEnabled) "Automatic session tracking is enabled"
                                        else "Enable location services for geofence tracking",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Session control buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val btnModifier = Modifier.weight(1f).height(56.dp)

                    if (sessionInProgress) {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = btnModifier,
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Time In", style = MaterialTheme.typography.titleMedium) }
                        Button(
                            onClick = {
                                val t = DateTimeFormatters.formatTime(System.currentTimeMillis())
                                dialogMessage = "End session at $t?"
                                recordedTimeAction = { viewModel.endSession() }
                                showDialog = true
                            },
                            modifier = btnModifier,
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Time Out", style = MaterialTheme.typography.titleMedium) }
                    } else {
                        Button(
                            onClick = {
                                val t = DateTimeFormatters.formatTime(System.currentTimeMillis())
                                dialogMessage = "Start session at $t?"
                                recordedTimeAction = { viewModel.startSession() }
                                showDialog = true
                            },
                            modifier = btnModifier,
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Start Session", style = MaterialTheme.typography.titleMedium) }
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = btnModifier,
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("End Session", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }

            if (sessionInProgress) {
                item {
                    Text(
                        "Session in progress...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Attendance log card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating AI Chat Button
        ExtendedFloatingActionButton(
            onClick = { navController?.navigate("aiAssistant") },
            icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Chat") },
            text = { Text("AI Chat") },
            expanded = isExpanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Action") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    recordedTimeAction?.invoke()
                    showDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
