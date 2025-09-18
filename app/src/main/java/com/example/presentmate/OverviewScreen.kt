package com.example.presentmate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
// import androidx.compose.material3.ExperimentalMaterial3Api // Unused import
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
// import androidx.navigation.NavHostController // Unused as navController parameter is removed
import com.example.presentmate.db.AppDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

data class DailySummary(
    val date: LocalDate,
    val totalDurationMillis: Long
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
fun OverviewScreen(/*navController: NavHostController*/) { // Removed unused navController
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val attendanceRecords by db.attendanceDao().getAllRecords().collectAsState(initial = emptyList())

    val dailySummaries = remember(attendanceRecords) {
        attendanceRecords
            .filter { it.timeIn != null && it.timeOut != null && it.timeOut > it.timeIn }
            .groupBy {
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .map { (date, recordsOnDate) ->
                val totalDuration = recordsOnDate.sumOf { it.timeOut!! - it.timeIn!! }
                DailySummary(date, totalDuration)
            }
            .sortedByDescending { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (dailySummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No attendance data to display.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Daily Total Durations", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dailySummaries, key = { it.date.toEpochDay() }) { summary ->
                    DailySummaryItem(summary = summary)
                }
            }
        }
    }
}

@Composable
fun DailySummaryItem(summary: DailySummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = summary.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = summary.durationString,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
