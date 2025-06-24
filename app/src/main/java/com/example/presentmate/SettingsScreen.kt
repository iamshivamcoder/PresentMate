package com.example.presentmate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    val deletedRecords by db.attendanceDao().getAllDeletedRecords().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        // Recycle Bin Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recycle Bin",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${deletedRecords.size} items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Deleted attendance records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deletedRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedRecords) { deletedRecord ->
                            DeletedRecordItem(
                                record = deletedRecord,
                                onRestore = { record ->
                                    scope.launch {
                                        db.attendanceDao().insertRecord(
                                            AttendanceRecord(
                                                id = record.originalId,
                                                date = record.date,
                                                timeIn = record.timeIn,
                                                timeOut = record.timeOut
                                            )
                                        )
                                        db.attendanceDao().permanentlyDeleteRecord(record.id)
                                    }
                                },
                                onPermanentDelete = { record ->
                                    scope.launch {
                                        db.attendanceDao().permanentlyDeleteRecord(record.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}

@Composable
fun DeletedRecordItem(
    record: com.example.presentmate.db.DeletedRecord,
    onRestore: (com.example.presentmate.db.DeletedRecord) -> Unit,
    onPermanentDelete: (com.example.presentmate.db.DeletedRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(java.util.Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${record.timeIn?.let { timeFormat.format(java.util.Date(it)) } ?: "N/A"} - ${
                        record.timeOut?.let { timeFormat.format(java.util.Date(it)) } ?: "N/A"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { onRestore(record) }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Restore,
                        contentDescription = "Restore record",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onPermanentDelete(record) }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Permanently delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
