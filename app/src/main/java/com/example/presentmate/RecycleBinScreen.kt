package com.example.presentmate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// import androidx.navigation.NavHostController // Unused import
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.db.DeletedRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class) // Retained for Card, etc.
@Composable
fun RecycleBinScreen(/*navController: NavHostController*/) { // Removed unused navController
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()
    val deletedRecords by db.attendanceDao().getAllDeletedRecords().collectAsState(initial = emptyList())

    // Removed Scaffold and TopAppBar as this is handled by MainActivity's Scaffold
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Direct padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (deletedRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Recycle Bin is empty", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("${deletedRecords.size} item(s)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deletedRecords, key = { it.id }) { deletedRecord ->
                    DeletedRecordItem(
                        record = deletedRecord,
                        onRestore = {
                            scope.launch {
                                db.attendanceDao().insertRecord(
                                    AttendanceRecord(
                                        id = it.originalId, 
                                        date = it.date,
                                        timeIn = it.timeIn,
                                        timeOut = it.timeOut
                                    )
                                )
                                db.attendanceDao().permanentlyDeleteRecord(it.id)
                            }
                        },
                        onPermanentDelete = {
                            scope.launch {
                                db.attendanceDao().permanentlyDeleteRecord(it.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeletedRecordItem(
    modifier: Modifier = Modifier,
    record: DeletedRecord,
    onRestore: (DeletedRecord) -> Unit,
    onPermanentDelete: (DeletedRecord) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    text = dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${record.timeIn?.let { timeFormat.format(Date(it)) } ?: "N/A"} → ${record.timeOut?.let { timeFormat.format(Date(it)) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { onRestore(record) }) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = "Restore record",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onPermanentDelete(record) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Permanently delete record",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
