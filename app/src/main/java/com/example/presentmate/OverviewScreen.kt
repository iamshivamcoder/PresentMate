package com.example.presentmate

// Import graph components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DailySummary(
    val date: LocalDate,
    val totalDurationMillis: Long,
    val records: List<AttendanceRecord> 
) {
    val durationString: String
        get() {
            if (totalDurationMillis <= 0) return "0m"
            val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMillis) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }
}

@Composable
fun OverviewScreen() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val attendanceRecords by db.attendanceDao().getAllRecords().collectAsState(initial = emptyList())

    var selectedGraphViewType by remember { mutableStateOf(GraphViewType.WEEKLY) }
    var currentDisplayDate by remember { mutableStateOf(LocalDate.now()) }

    val dailySummaries = remember(attendanceRecords) {
        attendanceRecords
            .filter { it.timeIn != null && it.timeOut != null && it.timeOut > it.timeIn }
            .groupBy { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { (date, recordsOnDate) ->
                val totalDuration = recordsOnDate.sumOf { it.timeOut!! - it.timeIn!! }
                DailySummary(date, totalDuration, recordsOnDate)
            }
            .sortedByDescending { it.date }
    }

    val graphData = remember(attendanceRecords, selectedGraphViewType, currentDisplayDate) {
        calculateGraphData(attendanceRecords, selectedGraphViewType, currentDisplayDate)
    }

    val stats = remember(attendanceRecords, graphData) {
        val totalHours = graphData.map { it.value }.sum()
        val averageHours = if (graphData.isNotEmpty()) totalHours / graphData.size else 0f
        val bestDay = graphData.maxByOrNull { it.value }?.label ?: "-"
        val goalProgress = totalHours // You can adjust this if you have a goal value
        GraphStats(totalHours, averageHours, bestDay, goalProgress)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GraphSection(
            viewType = selectedGraphViewType,
            displayDate = currentDisplayDate,
            data = graphData,
            stats = stats,
            onViewTypeChange = { selectedGraphViewType = it },
            onDateChange = { currentDisplayDate = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (dailySummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No attendance data to display.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Daily Breakdown", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dailySummaries, key = { it.date.toEpochDay() }) { summary ->
                    DailySummaryItem(summary = summary)
                }
            }
        }
    }
}

@Composable
fun DailySummaryItem(summary: DailySummary, modifier: Modifier = Modifier) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    CollapsibleCard(
        modifier = modifier,
        headerContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = summary.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = summary.durationString,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        collapsibleContent = {
            Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                if (summary.records.isEmpty()) {
                    Text("No individual records for this day.", style = MaterialTheme.typography.bodySmall)
                } else {
                    summary.records.sortedBy { it.timeIn }.forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Session:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${timeFormatter.format(record.timeIn)} - ${record.timeOut?.let { timeFormatter.format(it) } ?: "Ongoing"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    )
}
