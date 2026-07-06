package com.example.presentmate.ui.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.common.CollapsibleCard
import com.example.presentmate.ui.components.GraphSection
import com.example.presentmate.viewmodel.OverviewViewModel
import com.example.presentmate.utils.DateTimeFormatters

import java.time.LocalDate
import java.time.YearMonth
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

    // Group daily summaries by YearMonth for collapsible sections
    val summariesByMonth = remember(uiState.dailySummaries) {
        uiState.dailySummaries.groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
    }
    // Track expanded state per month — default all expanded
    val expandedMonths = remember { mutableStateMapOf<YearMonth, Boolean>() }

    var selectedDailySummary by remember { mutableStateOf<DailySummary?>(null) }

    val monthFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
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
        }

        // ── Study Trends & Distribution (ABOVE Daily Breakdown) ───────────────
        if (uiState.dailySummaries.isNotEmpty() && uiState.graphData.isNotEmpty()) {
            item {
                Text(
                    text = "Study Trends & Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Trend (Hours)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        val lineChartGoal = if (uiState.selectedGraphViewType == com.example.presentmate.ui.components.GraphViewType.YEARLY) {
                            (weeklyGoalHours * 52f) / 12f
                        } else {
                            weeklyGoalHours / 7f
                        }
                        com.example.presentmate.ui.components.EnhancedLineChart(data = uiState.graphData, animationPlayed = true, studyGoal = lineChartGoal)
                    }
                }

                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Time of Day Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        val allRecords = uiState.dailySummaries.flatMap { it.records }
                        val cal = java.util.Calendar.getInstance()
                        var morning = 0f
                        var afternoon = 0f
                        var evening = 0f

                        allRecords.forEach { r ->
                            cal.timeInMillis = r.timeIn ?: System.currentTimeMillis()
                            val hr = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val dur = ((r.timeOut ?: System.currentTimeMillis()) - (r.timeIn ?: System.currentTimeMillis())) / 3600000f
                            when {
                                hr < 12 -> morning += dur
                                hr in 12..16 -> afternoon += dur
                                else -> evening += dur
                            }
                        }

                        if (morning > 0 || afternoon > 0 || evening > 0) {
                            com.example.presentmate.ui.components.EnhancedPieChart(
                                values = listOf(morning, afternoon, evening),
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                ),
                                labels = listOf("Morning", "Afternoon", "Evening")
                            )
                        } else {
                            Text("No time data available for distribution chart.")
                        }
                    }
                }
            }
        }

        // ── Daily Breakdown (month-grouped, collapsible) ───────────────────────
        if (uiState.dailySummaries.isEmpty()) {
            item {
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
            }
        } else {
            item {
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            summariesByMonth.forEach { (month, summaries) ->
                val isExpanded = expandedMonths.getOrPut(month) { true }
                item(key = month.toString()) {
                    MonthHeader(
                        label = month.format(monthFmt),
                        totalMillis = summaries.sumOf { it.totalDurationMillis },
                        count = summaries.size,
                        isExpanded = isExpanded,
                        onToggle = { expandedMonths[month] = !isExpanded }
                    )
                }
                if (isExpanded) {
                    items(summaries, key = { it.date.toString() }) { summary ->
                        AnimatedVisibility(
                            visible = true,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            DailySummaryItem(
                                summary = summary, 
                                modifier = Modifier.padding(bottom = 10.dp, start = 8.dp),
                                onClick = { selectedDailySummary = summary }
                            )
                        }
                    }
                }
                item(key = "${month}_spacer") { Spacer(Modifier.height(8.dp)) }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    selectedDailySummary?.let { summary ->
        com.example.presentmate.ui.components.DailyAnalysisBottomSheet(
            summary = summary,
            onDismissRequest = { selectedDailySummary = null }
        )
    }
}

@Composable
private fun MonthHeader(
    label: String,
    totalMillis: Long,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clickable(onClick = onToggle)
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$count day${if (count != 1) "s" else ""} · ${DateTimeFormatters.formatDuration(totalMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DailySummaryItem(summary: DailySummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summary.durationString,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
