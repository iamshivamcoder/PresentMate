package com.example.presentmate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceLogList(records: List<AttendanceRecord>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<AttendanceRecord?>(null) }
    var editedTimeInText by remember { mutableStateOf("") }
    var editedTimeOutText by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No attendance records yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    if (showEditDialog && recordToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Session") },
            text = {
                Column {
                    Text("Date: ${SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(recordToEdit!!.date))}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Time In:")
                    TextField(
                        value = editedTimeInText,
                        onValueChange = { editedTimeInText = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Time Out:")
                    TextField(
                        value = editedTimeOutText,
                        onValueChange = { editedTimeOutText = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val parsedTimeIn = try { timeFormat.parse(editedTimeInText)?.time } catch (_: Exception) { null }
                            val parsedTimeOut = try { timeFormat.parse(editedTimeOutText)?.time } catch (_: Exception) { null }
                            db.attendanceDao().updateRecord(
                                recordToEdit!!.copy(
                                    timeIn = parsedTimeIn,
                                    timeOut = parsedTimeOut
                                )
                            )
                            showEditDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this attendance record?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        // Move to recycle bin before deleting
                        db.attendanceDao().insertDeletedRecord(
                            com.example.presentmate.db.DeletedRecord(
                                originalId = recordToDelete!!.id,
                                date = recordToDelete!!.date,
                                timeIn = recordToDelete!!.timeIn,
                                timeOut = recordToDelete!!.timeOut,
                                deletedAt = System.currentTimeMillis()
                            )
                        )
                        db.attendanceDao().deleteRecord(recordToDelete!!)
                        showDeleteDialog = false
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Changed from 8.dp to 16.dp
    ) {
        items(records) { record ->
            AttendanceRecordItem(
                record = record,
                onEdit = { recordToEditParam ->
                    recordToEdit = recordToEditParam
                    editedTimeInText = recordToEditParam.timeIn?.let { timeFormat.format(Date(it)) } ?: ""
                    editedTimeOutText = recordToEditParam.timeOut?.let { timeFormat.format(Date(it)) } ?: ""
                    showEditDialog = true
                },
                onDelete = { recordToDeleteParam ->
                    recordToDelete = recordToDeleteParam
                    showDeleteDialog = true
                }
            )
        }
    }
}

@Composable
fun AttendanceRecordItem(
    modifier: Modifier = Modifier,
    record: AttendanceRecord,
    onEdit: (AttendanceRecord) -> Unit = {},
    onDelete: (AttendanceRecord) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dateFormat.format(Date(record.date)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Time In: ${record.timeIn?.let { timeFormat.format(Date(it)) } ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Time Out: ${record.timeOut?.let { timeFormat.format(Date(it)) } ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    IconButton(onClick = { onEdit(record) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit record",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDelete(record) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete record",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
