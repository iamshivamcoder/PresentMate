package com.example.presentmate.ui.screens

// import androidx.navigation.NavHostController // Unused
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.presentmate.db.AttendanceLogList
import com.example.presentmate.MotivationalAnimation
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier
    // navController: NavHostController? = null // Unused
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
                        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                        dialogMessage = "Start session at $currentTime?"
                        recordedTimeAction = {
                            scope.launch {
                                val now = System.currentTimeMillis()
                                db.attendanceDao().insertRecord(
                                    AttendanceRecord(date = now, timeIn = now, timeOut = null)
                                )
                            }
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
