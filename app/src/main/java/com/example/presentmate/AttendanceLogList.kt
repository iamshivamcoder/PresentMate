package com.example.presentmate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import java.time.Instant
// import java.time.LocalDate // Unused import
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
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

    val recordsByDate = remember(records) {
        records.groupBy {
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSortedMap(compareByDescending { it }) // Sort by date descending
    }

    if (recordsByDate.isEmpty()) {
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
        val recordDate = Instant.ofEpochMilli(recordToEdit!!.date).atZone(ZoneId.systemDefault()).toLocalDate()
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Session") },
            text = {
                Column {
                    Text("Date: ${recordDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))}")
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
        modifier = modifier
    ) {
        recordsByDate.forEach { (date, recordsForDate) ->
            stickyHeader {
                Text(
                    text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant) 
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(recordsForDate, key = { it.id }) { record ->
                AttendanceRecordItem(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
}

@Composable
fun AttendanceRecordItem(
    modifier: Modifier = Modifier,
    record: AttendanceRecord,
    onEdit: (AttendanceRecord) -> Unit = {},
    onDelete: (AttendanceRecord) -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val durationString = remember(record.timeIn, record.timeOut) {
        val timeIn = record.timeIn
        val timeOut = record.timeOut
        if (timeIn != null && timeOut != null && timeOut > timeIn) {
            val diff = timeOut - timeIn
            val minutes = (diff / (1000 * 60)) % 60
            val hours = (diff / (1000 * 60 * 60))
            when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m" 
            }
        } else {
            ""
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Changed to surface for item
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Adjusted elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${record.timeIn?.let { timeFormat.format(Date(it)) } ?: "N/A"} → ${record.timeOut?.let { timeFormat.format(Date(it)) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (durationString.isNotEmpty()) {
                    Text(
                        text = "Duration: $durationString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
