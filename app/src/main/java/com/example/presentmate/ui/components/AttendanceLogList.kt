package com.example.presentmate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.db.DeletedRecord
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.utils.DateTimeFormatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Composable
fun AttendanceLogList(records: List<AttendanceRecord>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { PresentMateDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<AttendanceRecord?>(null) }
    var editedTimeInText by remember { mutableStateOf("") }
    var editedTimeOutText by remember { mutableStateOf("") }
    val timeFormat = remember { DateTimeFormatters.timeFormat }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    val recordsByDate = remember(records) {
        records.groupBy {
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    if (recordsByDate.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
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
                            database.attendanceDao().updateRecord(
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
                        database.attendanceDao().insertDeletedRecord(
                            DeletedRecord(
                                originalId = recordToDelete!!.id,
                                date = recordToDelete!!.date,
                                timeIn = recordToDelete!!.timeIn,
                                timeOut = recordToDelete!!.timeOut,
                                deletedAt = System.currentTimeMillis()
                            )
                        )
                        database.attendanceDao().deleteRecord(recordToDelete!!)
                        showDeleteDialog = false
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier) {
        recordsByDate.forEach { (date, recordsForDate) ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            recordsForDate.forEach { record ->
                AttendanceRecordItem(
                    record = record,
                    onEdit = { r ->
                        recordToEdit = r
                        editedTimeInText = r.timeIn?.let { timeFormat.format(Date(it)) } ?: ""
                        editedTimeOutText = r.timeOut?.let { timeFormat.format(Date(it)) } ?: ""
                        showEditDialog = true
                    },
                    onDelete = { r ->
                        recordToDelete = r
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun AttendanceRecordItem(
    record: AttendanceRecord,
    onEdit: (AttendanceRecord) -> Unit = {},
    onDelete: (AttendanceRecord) -> Unit = {}
) {
    val timeFormat = remember { DateTimeFormatters.timeFormat }
    val timelineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    val dotRadius = 5.dp
    val lineWidth = 2.dp

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
        } else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                drawLine(
                    color = timelineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = lineWidth.toPx()
                )
                drawCircle(
                    color = dotColor,
                    radius = dotRadius.toPx(),
                    center = Offset(centerX, 28.dp.toPx())
                )
            }
        }

        // Content card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = record.timeIn?.let { timeFormat.format(Date(it)) } ?: "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " → ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = record.timeOut?.let { timeFormat.format(Date(it)) } ?: "Ongoing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (record.timeOut == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (durationString.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = durationString,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { onEdit(record) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(record) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

