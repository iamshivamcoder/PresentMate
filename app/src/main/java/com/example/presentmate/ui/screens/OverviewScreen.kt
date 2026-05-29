package com.example.presentmate.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Context
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.common.CollapsibleCard
import com.example.presentmate.ui.components.GraphSection
import com.example.presentmate.viewmodel.OverviewViewModel
import com.example.presentmate.utils.DateTimeFormatters

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

@androidx.compose.runtime.Stable
data class DailySummary(
    val date: LocalDate,
    val totalDurationMillis: Long,
    val records: List<AttendanceRecord>
) {
    val durationString: String
        get() = DateTimeFormatters.formatDuration(totalDurationMillis)
}

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("overview_prefs", Context.MODE_PRIVATE) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var weeklyGoalHours by remember { mutableFloatStateOf(prefs.getFloat("weekly_goal_hours", 10f)) }

    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            val newGoal = prefs.getFloat("weekly_goal_hours", 10f)
            if (weeklyGoalHours != newGoal) {
                weeklyGoalHours = newGoal
                viewModel.onDateChange(uiState.currentDisplayDate)
            } else {
                weeklyGoalHours = newGoal
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GraphSection(
            viewType = uiState.selectedGraphViewType,
            displayDate = uiState.currentDisplayDate,
            data = uiState.graphData,
            stats = uiState.stats,
            onViewTypeChange = { viewModel.onViewTypeChange(it) },
            onDateChange = { viewModel.onDateChange(it) },
            weeklyGoalHours = weeklyGoalHours
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.dailySummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attendance data yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Daily Breakdown", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            uiState.dailySummaries.forEach { summary ->
                DailySummaryItem(summary = summary, modifier = Modifier.padding(bottom = 10.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Study Trends & Distribution", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trend (Hours)", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    com.example.presentmate.ui.components.EnhancedLineChart(data = uiState.graphData, animationPlayed = true)
                }
            }
            
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Time of Day Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    val allRecords = uiState.dailySummaries.flatMap { it.records }
                    val cal = java.util.Calendar.getInstance()
                    var morning = 0f
                    var afternoon = 0f
                    var evening = 0f
                    
                    allRecords.forEach { r ->
                        cal.timeInMillis = r.timeIn
                        val hr = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val dur = ((r.timeOut ?: System.currentTimeMillis()) - r.timeIn) / 3600000f
                        when {
                            hr < 12 -> morning += dur
                            hr in 12..16 -> afternoon += dur
                            else -> evening += dur
                        }
                    }
                    
                    com.example.presentmate.ui.components.EnhancedPieChart(
                        values = listOf(morning, afternoon, evening),
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        ),
                        labels = listOf("Morning", "Afternoon", "Evening")
                    )
                }
            }
        }
    }
}

@Composable
fun DailySummaryItem(summary: DailySummary, modifier: Modifier = Modifier) {
    val timeFormatter = remember { DateTimeFormatters.timeFormat }

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
            Column(
                modifier = Modifier.padding(
                    top = 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
            ) {
                if (summary.records.isEmpty()) {
                    Text(
                        "No individual records for this day.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    summary.records.sortedBy { it.timeIn ?: 0L }.forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Session:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${record.timeIn?.let { timeFormatter.format(it) } ?: "N/A"} - ${
                                    record.timeOut?.let {
                                        timeFormatter.format(
                                            it
                                        )
                                    } ?: "Ongoing"
                                }",
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
