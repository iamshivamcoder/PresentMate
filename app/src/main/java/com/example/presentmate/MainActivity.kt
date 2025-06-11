package com.example.presentmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.presentmate.ui.theme.PresentMateTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PresentMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AttendanceScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceScreen(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var recordedTimeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    val attendanceRecords by db.attendanceDao().getAllRecords().collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            dialogMessage = "Confirm Time In at $currentTime?"
            recordedTimeAction = {
                scope.launch {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val existingRecord = db.attendanceDao().getRecordByDate(today)
                    if (existingRecord != null) {
                        db.attendanceDao().updateRecord(existingRecord.copy(timeIn = System.currentTimeMillis()))
                    } else {
                        db.attendanceDao().insertRecord(AttendanceRecord(date = today, timeIn = System.currentTimeMillis()))
                    }
                }
            }
            showDialog = true
        }) {
            Text("Time In")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            dialogMessage = "Confirm Time Out at $currentTime?"
            recordedTimeAction = {
                scope.launch {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val existingRecord = db.attendanceDao().getRecordByDate(today)
                    if (existingRecord != null) {
                        db.attendanceDao().updateRecord(existingRecord.copy(timeOut = System.currentTimeMillis()))
                    } else {
                        // Optionally handle case where Time Out is pressed before Time In for the day
                        // For now, we'll assume Time In was already recorded or create a new record with only Time Out
                        db.attendanceDao().insertRecord(AttendanceRecord(date = today, timeOut = System.currentTimeMillis()))
                    }
                }
            }
            showDialog = true
        }) {
            Text("Time Out")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Attendance Log", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        AttendanceLogList(records = attendanceRecords)
    }

    if (showDialog) {
        ConfirmationDialog(
            dialogMessage = dialogMessage,
            onConfirm = {
                recordedTimeAction?.invoke()
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}

@Composable
fun ConfirmationDialog(
    dialogMessage: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Confirm Action")
        },
        text = {
            Text(text = dialogMessage)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AttendanceScreenPreview() {
    PresentMateTheme {
        AttendanceScreen()
    }
}

@Composable
fun AttendanceLogList(records: List<AttendanceRecord>, modifier: Modifier = Modifier) {
    if (records.isEmpty()) {
        Text("No attendance records yet.")
        return
    }
    LazyColumn(modifier = modifier) {
        items(records) {
            record ->
            AttendanceRecordItem(record = record)
            Divider()
        }
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date: ${dateFormat.format(Date(record.date))}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Time In: ${record.timeIn?.let { timeFormat.format(Date(it)) } ?: "N/A"}")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Time Out: ${record.timeOut?.let { timeFormat.format(Date(it)) } ?: "N/A"}")
        }
    }
}