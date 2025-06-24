package com.example.presentmate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.AttendanceLogList
import com.example.presentmate.MotivationalAnimation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var recordedTimeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    val attendanceRecords by db.attendanceDao().getAllRecords().collectAsState(initial = emptyList())
    val ongoingSession = attendanceRecords.lastOrNull { it.timeOut == null }
    val sessionInProgress = ongoingSession != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    dialogMessage = "Start session at $currentTime?"
                    recordedTimeAction = {
                        scope.launch {
                            if (!sessionInProgress) {
                                val now = System.currentTimeMillis()
                                db.attendanceDao().insertRecord(
                                    AttendanceRecord(date = now, timeIn = now, timeOut = null)
                                )
                            }
                        }
                    }
                    showDialog = true
                },
                enabled = !sessionInProgress,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Time In", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = {
                    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    dialogMessage = "End session at $currentTime?"
                    recordedTimeAction = {
                        scope.launch {
                            ongoingSession?.let {
                                db.attendanceDao().updateRecord(
                                    it.copy(timeOut = System.currentTimeMillis())
                                )
                            }
                        }
                    }
                    showDialog = true
                },
                enabled = sessionInProgress,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Time Out", style = MaterialTheme.typography.titleMedium)
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
