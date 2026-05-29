package com.example.presentmate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
// import java.time.format.TextStyle // Renamed to avoid conflict with androidx.compose.ui.text.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class GraphViewType { WEEKLY, MONTHLY, YEARLY }

@androidx.compose.runtime.Stable
data class GraphDataPoint(
    val label: String,
    val value: Float, // Total hours
    val rawMillis: Long = 0L,
    val isToday: Boolean = false,
    val hasGoal: Boolean = false,
    val goalValue: Float = 0f
)

@androidx.compose.runtime.Stable
data class GraphStats(
    val totalHours: Float,
    val averageHours: Float,
    val bestDay: String,
    val goalProgress: Float
)

fun formatMillisToHours(millis: Long): Float {
    return millis / (1000f * 60 * 60)
}

@Composable
fun GraphSection(
    viewType: GraphViewType,
    displayDate: LocalDate,
    data: List<GraphDataPoint>,
    stats: GraphStats,
    onViewTypeChange: (GraphViewType) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    weeklyGoalHours: Float = 10f // Default weekly study goal
) {
    var animationPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        animationPlayed = false
        delay(100) // Small delay to ensure recomposition before animation
        animationPlayed = true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.totalHours.format(1)}h total • ${stats.averageHours.format(1)}h avg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Analytics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphViewType.entries.forEach { type ->
                    FilterChip(
                        selected = viewType == type,
                        onClick = { onViewTypeChange(type) },
                        label = {
                            Text(
                                type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newDate = when (viewType) {
                            GraphViewType.WEEKLY -> displayDate.minusWeeks(1)
                            GraphViewType.MONTHLY -> displayDate.minusMonths(1)
                            GraphViewType.YEARLY -> displayDate.minusYears(1)
                        }
                        onDateChange(newDate)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Period"
                    )
                }
                Text(
                    text = when (viewType) {
                        GraphViewType.WEEKLY -> "Week of ${displayDate.with(WeekFields.of(Locale.getDefault()).firstDayOfWeek).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
                        GraphViewType.MONTHLY -> displayDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                        GraphViewType.YEARLY -> displayDate.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = {
                        val newDate = when (viewType) {
                            GraphViewType.WEEKLY -> displayDate.plusWeeks(1)
                            GraphViewType.MONTHLY -> displayDate.plusMonths(1)
                            GraphViewType.YEARLY -> displayDate.plusYears(1)
                        }
                        onDateChange(newDate)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Period"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (data.isEmpty() || data.all { it.rawMillis == 0L }) {
                EmptyStateCard()
            } else {
                EnhancedBarChart(
                    data = data,
                    animationPlayed = animationPlayed,
                    studyGoal = if (viewType == GraphViewType.YEARLY) (weeklyGoalHours * 52f) / 12f else weeklyGoalHours / 7f
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GoalProgressIndicator(
                currentProgress = stats.totalHours,
                goal = when (viewType) {
                    GraphViewType.WEEKLY -> weeklyGoalHours
                    GraphViewType.MONTHLY -> weeklyGoalHours * (displayDate.lengthOfMonth() / 7f)
                    GraphViewType.YEARLY -> weeklyGoalHours * 52f
                },
                viewType = viewType
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "No data",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "No study data yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Start tracking your study sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun GoalProgressIndicator(
    currentProgress: Float,
    goal: Float,
    viewType: GraphViewType
) {
    val progressPercentage = if (goal > 0) (currentProgress / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${viewType.name.lowercase().replaceFirstChar { it.uppercase() }} Goal Progress",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${currentProgress.format(1)}h / ${goal.format(0)}h",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun EnhancedBarChart(
    data: List<GraphDataPoint>,
    animationPlayed: Boolean,
    studyGoal: Float,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = maxOf(data.maxOfOrNull { it.value } ?: 1f, studyGoal)
    var selectedBarIndex by remember { mutableIntStateOf(-1) }

    // Hoist color scheme calls to the composable scope
    val themeSecondaryColor = MaterialTheme.colorScheme.secondary
    val themePrimaryColor = MaterialTheme.colorScheme.primary
    val themeOnSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Hoist TextStyle definitions
    val selectedTextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val defaultTextStyle = MaterialTheme.typography.labelSmall

    val barProperties = remember(data, animationPlayed, selectedBarIndex, studyGoal, maxVal, themeSecondaryColor, themePrimaryColor) {
        data.mapIndexed { index, dataPoint ->
            val targetAnimatedHeightFactor = if (animationPlayed) (dataPoint.value / maxVal) else 0f
            val baseColor = when {
                dataPoint.isToday -> themeSecondaryColor // Use hoisted color
                dataPoint.value >= studyGoal -> themePrimaryColor
                dataPoint.value >= studyGoal * 0.7f -> themePrimaryColor.copy(alpha = 0.8f) // Use hoisted color
                else -> themePrimaryColor.copy(alpha = 0.5f) // Use hoisted color
            }
            val isSelected = selectedBarIndex == index
            val barColor = if (isSelected) baseColor.copy(alpha = 1f) else baseColor

            object {
                val targetHeightFactor = targetAnimatedHeightFactor
                val color = barColor
            }
        }
    }

    val animatedHeightFactors = data.mapIndexed { index, _ ->
        val properties = barProperties[index]
        animateFloatAsState(
            targetValue = properties.targetHeightFactor,
            animationSpec = tween(durationMillis = 800, delayMillis = index * 100, easing = LinearEasing),
            label = "barHeightFactor$index"
        ).value
    }

    // Hoist grid color
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val chartWidth = size.width
                val chartHeight = size.height - 60.dp.toPx()
                val maxBarWidth = 40.dp.toPx()
                val rawBarWidth = chartWidth / data.size * 0.7f
                val barWidth = minOf(rawBarWidth, maxBarWidth)
                val totalBarsWidth = barWidth * data.size
                val barSpacing = (chartWidth - totalBarsWidth) / data.size

                val gridLines = 5
                // Retrieve onSurface color directly, but we can't do MaterialTheme inside Canvas, so we hoisted it earlier? Wait, we didn't hoist it, we can just use Color.Gray but we really should use MaterialTheme.
                // Let's use Color.Gray since we can't use composable inside Canvas easily without hoisting. Wait, I will use Color.Gray but with less opacity, or hoist it. I'll hoist it above.
                for (i in 1..gridLines) {
                    val yLine = chartHeight * (i / gridLines.toFloat())
                    drawLine(
                        color = gridColor, start = Offset(0f, yLine),
                        end = Offset(chartWidth, yLine), strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                }

                if (studyGoal > 0) {
                    val goalY = chartHeight - (studyGoal / maxVal * chartHeight)
                    if (goalY >= 0 && goalY <= chartHeight) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.6f),
                            start = Offset(0f, goalY), end = Offset(chartWidth, goalY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                        )
                    }
                }

                data.forEachIndexed { index, _ ->
                    val properties = barProperties[index]
                    val barActualHeight = animatedHeightFactors[index] * chartHeight
                    val x = index * (barWidth + barSpacing) + barSpacing / 2
                    val y = chartHeight - barActualHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(colors = listOf(properties.color, properties.color.copy(alpha = 0.6f))),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barActualHeight.coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    if (selectedBarIndex == index) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                            size = Size(barWidth + 2.dp.toPx(), barActualHeight + 2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEachIndexed { index, _ ->
                    Box(modifier = Modifier.weight(1f).fillMaxSize().clickable { selectedBarIndex = if (selectedBarIndex == index) -1 else index })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, dataPoint ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelected = selectedBarIndex == index
                    val valueColor by animateColorAsState(
                        targetValue = if (isSelected) themePrimaryColor else themeOnSurfaceVariantColor,
                        label = "valueColor$index"
                    )
                    Text(
                        text = if (isSelected) {
                            "${dataPoint.value.format(1)}h"
                        } else if (data.size <= 7) {
                            "${dataPoint.value.format(1)}h"
                        } else "",
                        style = if (isSelected) selectedTextStyle else defaultTextStyle, // Use hoisted styles
                        color = valueColor, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (data.size > 14) {
                            // Logic to skip labels for dense data (Monthly)
                            if (index % 5 == 0 || index == data.size - 1) dataPoint.label else ""
                        } else {
                            dataPoint.label
                        },
                        style = defaultTextStyle, // Use hoisted style
                        color = themeOnSurfaceVariantColor,
                        textAlign = TextAlign.Center, 
                        maxLines = 1
                    )
                    if (dataPoint.isToday) {
                        Box(modifier = Modifier.padding(top = 2.dp).size(4.dp).background(themeSecondaryColor, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            LegendItem(color = themePrimaryColor, label = "Study Time")
            if (studyGoal > 0) {
                LegendItem(color = Color.Red.copy(alpha = 0.6f), label = "Daily Goal (${studyGoal.format(1)}h)", isDashed = true)
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant // Hoist for LegendItem
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isDashed) {
            Canvas(modifier = Modifier.size(16.dp, 2.dp)) {
                drawLine(
                    color = color, start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 2f))
                )
            }
        } else {
            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
    }
}

fun Float.format(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}

fun calculateGraphData(
    records: List<AttendanceRecord>,
    viewType: GraphViewType,
    displayDate: LocalDate
): List<GraphDataPoint> {
    val locale = Locale.getDefault()
    val today = LocalDate.now()
    
    // O(N) single pass to map records to local dates and sum durations
    val dailyDurations = mutableMapOf<LocalDate, Long>()
    records.forEach { record ->
        if (record.timeIn != null && record.timeOut != null && record.timeOut > record.timeIn) {
            val date = Instant.ofEpochMilli(record.date).atZone(ZoneId.systemDefault()).toLocalDate()
            val duration = record.timeOut - record.timeIn
            dailyDurations[date] = (dailyDurations[date] ?: 0L) + duration
        }
    }

    return when (viewType) {
        GraphViewType.WEEKLY -> {
            val weekFields = WeekFields.of(locale)
            val startOfWeek = displayDate.with(weekFields.dayOfWeek(), 1L)
            (0..6).map { dayIndex ->
                val currentDay = startOfWeek.plusDays(dayIndex.toLong())
                val totalMillis = dailyDurations[currentDay] ?: 0L
                GraphDataPoint(
                    label = currentDay.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    value = formatMillisToHours(totalMillis), rawMillis = totalMillis, isToday = currentDay.isEqual(today)
                )
            }
        }
        GraphViewType.MONTHLY -> {
            val startOfMonth = displayDate.withDayOfMonth(1)
            val daysInMonth = startOfMonth.lengthOfMonth()
            (1..daysInMonth).map { dayOfMonth ->
                val currentDay = startOfMonth.withDayOfMonth(dayOfMonth)
                val totalMillis = dailyDurations[currentDay] ?: 0L
                GraphDataPoint(
                    label = dayOfMonth.toString(),
                    value = formatMillisToHours(totalMillis), rawMillis = totalMillis, isToday = currentDay.isEqual(today)
                )
            }
        }
        GraphViewType.YEARLY -> {
            (1..12).map { monthIndex ->
                val currentMonth = displayDate.withMonth(monthIndex)
                // Filter dailyDurations for this month and sum
                val totalMillis = dailyDurations.filterKeys { it.year == currentMonth.year && it.monthValue == monthIndex }.values.sum()
                GraphDataPoint(
                    label = currentMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    value = formatMillisToHours(totalMillis), rawMillis = totalMillis,
                    isToday = currentMonth.year == today.year && currentMonth.month == today.month
                )
            }
        }
    }
}
