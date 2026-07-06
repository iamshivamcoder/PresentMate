package com.example.presentmate.ui.components


import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow

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
import androidx.compose.foundation.layout.width
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
    
    val haptic = LocalHapticFeedback.current

    val successColor = Color(0xFF4CAF50)
    val failureColor = Color(0xFFF44336)
    val themeSecondaryColor = MaterialTheme.colorScheme.secondary
    val themeOnSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedTextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val defaultTextStyle = MaterialTheme.typography.labelSmall

    val maxDataValue = data.maxOfOrNull { it.value } ?: 0f
    val minDataValue = data.minOfOrNull { it.value } ?: 0f

    val barProperties = remember(data, animationPlayed, selectedBarIndex, studyGoal, maxVal) {
        data.mapIndexed { index, dataPoint ->
            val targetAnimatedHeightFactor = if (animationPlayed) (dataPoint.value / maxVal) else 0f
            val baseColor = if (studyGoal > 0f) {
                if (dataPoint.value >= studyGoal) successColor else failureColor
            } else {
                themeSecondaryColor
            }
            val isSelected = selectedBarIndex == index
            val barColor = if (isSelected) baseColor.copy(alpha = 1f) else baseColor.copy(alpha = 0.7f)

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
            animationSpec = tween(durationMillis = 800, delayMillis = index * 50, easing = LinearEasing),
            label = "barHeightFactor$index"
        ).value
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textMeasurer = rememberTextMeasurer()
    val badgeTextStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val chartWidth = size.width
                                val totalBarsWidth = (chartWidth / data.size * 0.7f).coerceAtMost(40.dp.toPx()) * data.size
                                val barSpacing = (chartWidth - totalBarsWidth) / data.size
                                val totalStep = (chartWidth / data.size * 0.7f).coerceAtMost(40.dp.toPx()) + barSpacing
                                val index = (offset.x / totalStep).toInt().coerceIn(0, data.size - 1)
                                if (selectedBarIndex != index) {
                                    selectedBarIndex = index
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDrag = { change, _ ->
                                val chartWidth = size.width
                                val totalBarsWidth = (chartWidth / data.size * 0.7f).coerceAtMost(40.dp.toPx()) * data.size
                                val barSpacing = (chartWidth - totalBarsWidth) / data.size
                                val totalStep = (chartWidth / data.size * 0.7f).coerceAtMost(40.dp.toPx()) + barSpacing
                                val index = (change.position.x / totalStep).toInt().coerceIn(0, data.size - 1)
                                if (selectedBarIndex != index) {
                                    selectedBarIndex = index
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = { selectedBarIndex = -1 },
                            onDragCancel = { selectedBarIndex = -1 }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height - 60.dp.toPx()
                val rawBarWidth = chartWidth / data.size * 0.7f
                val maxBarWidth = 40.dp.toPx()
                val barWidth = minOf(rawBarWidth, maxBarWidth)
                val totalBarsWidth = barWidth * data.size
                val barSpacing = (chartWidth - totalBarsWidth) / data.size

                for (i in 1..5) {
                    val yLine = chartHeight * (i / 5f)
                    drawLine(
                        color = gridColor, start = Offset(0f, yLine),
                        end = Offset(chartWidth, yLine), strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                }

                if (studyGoal > 0) {
                    val goalY = chartHeight - (studyGoal / maxVal * chartHeight)
                    if (goalY in 0f..chartHeight) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.6f),
                            start = Offset(0f, goalY), end = Offset(chartWidth, goalY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                        )
                    }
                }

                data.forEachIndexed { index, dataPoint ->
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

                    // Min/Max Badges
                    if (animationPlayed) {
                        if (dataPoint.value == maxDataValue && maxDataValue > 0) {
                            val badgeY = y - 16.dp.toPx()
                            drawRoundRect(color = Color(0xFFFF9800), topLeft = Offset(x, badgeY), size = Size(barWidth, 12.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
                            val textLayoutResult = textMeasurer.measure("MAX", badgeTextStyle)
                            drawText(textMeasurer, "MAX", topLeft = Offset(x + (barWidth - textLayoutResult.size.width) / 2f, badgeY), style = badgeTextStyle)
                        } else if (dataPoint.value == minDataValue && minDataValue >= 0 && dataPoint.value != maxDataValue) {
                            val badgeY = y - 16.dp.toPx()
                            drawRoundRect(color = Color(0xFF9E9E9E), topLeft = Offset(x, badgeY), size = Size(barWidth, 12.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
                            val textLayoutResult = textMeasurer.measure("MIN", badgeTextStyle)
                            drawText(textMeasurer, "MIN", topLeft = Offset(x + (barWidth - textLayoutResult.size.width) / 2f, badgeY), style = badgeTextStyle)
                        }
                    }

                    if (selectedBarIndex == index) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                            size = Size(barWidth + 2.dp.toPx(), barActualHeight + 2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                        // Tooltip over bar
                        val tooltipText = "${dataPoint.value.format(1)}h"
                        val ttLayout = textMeasurer.measure(tooltipText, selectedTextStyle)
                        drawRoundRect(color = Color.DarkGray, topLeft = Offset(x + barWidth/2 - ttLayout.size.width/2 - 8.dp.toPx(), y - 30.dp.toPx()), size = Size(ttLayout.size.width + 16.dp.toPx(), ttLayout.size.height + 8.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
                        drawText(textMeasurer, tooltipText, topLeft = Offset(x + barWidth/2 - ttLayout.size.width/2, y - 26.dp.toPx()), style = selectedTextStyle.copy(color = Color.White))
                    }
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
                    Text(
                        text = if (data.size > 14) {
                            if (index % 5 == 0 || index == data.size - 1) dataPoint.label else ""
                        } else {
                            dataPoint.label
                        },
                        style = defaultTextStyle,
                        color = if (isSelected) themeSecondaryColor else themeOnSurfaceVariantColor,
                        textAlign = TextAlign.Center, 
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            LegendItem(color = successColor, label = "Met Goal")
            LegendItem(color = failureColor, label = "Missed Goal")
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

@Composable
fun EnhancedLineChart(data: List<GraphDataPoint>, animationPlayed: Boolean, studyGoal: Float = 0f, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    
    val maxVal = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    val maxDataValue = data.maxOfOrNull { it.value } ?: 0f
    val minDataValue = data.minOfOrNull { it.value } ?: 0f
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "lineChartProgress"
    )
    val lineColor = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF4CAF50)
    val failureColor = Color(0xFFF44336)
    
    var scrubbedIndex by remember { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
                        val index = (offset.x / stepX).apply { Math.round(this) }.toInt().coerceIn(0, data.size - 1)
                        if (scrubbedIndex != index) {
                            scrubbedIndex = index
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDrag = { change, _ ->
                        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
                        val index = (change.position.x / stepX).apply { Math.round(this) }.toInt().coerceIn(0, data.size - 1)
                        if (scrubbedIndex != index) {
                            scrubbedIndex = index
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = { scrubbedIndex = -1 },
                    onDragCancel = { scrubbedIndex = -1 }
                )
            }
    ) {
        val width = size.width
        val height = size.height - 20.dp.toPx()
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        val path = androidx.compose.ui.graphics.Path()
        val points = mutableListOf<Offset>()

        data.forEachIndexed { i, dp ->
            val x = i * stepX
            val y = height - ((dp.value / maxVal) * height * animatedProgress)
            points.add(Offset(x, y))
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevPt = points[i - 1]
                val cp1x = prevPt.x + (x - prevPt.x) / 2f
                val cp1y = prevPt.y
                val cp2x = prevPt.x + (x - prevPt.x) / 2f
                val cp2y = y
                path.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx())
        )
        
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw points with goal-based colors and min/max highlights
        points.forEachIndexed { i, pt ->
            val dp = data[i]
            val pointColor = if (studyGoal > 0) {
                if (dp.value >= studyGoal) successColor else failureColor
            } else {
                lineColor
            }
            
            val isMin = dp.value == minDataValue && minDataValue >= 0f
            val isMax = dp.value == maxDataValue && maxDataValue > 0f
            
            if (isMax || isMin || scrubbedIndex == i) {
                val radius = if (scrubbedIndex == i) 6.dp.toPx() else 5.dp.toPx()
                drawCircle(color = pointColor, radius = radius, center = pt)
                drawCircle(color = Color.White, radius = radius / 2f, center = pt)
            } else {
                drawCircle(color = pointColor, radius = 3.dp.toPx(), center = pt)
            }
            
            // Text Badges for Min/Max
            if ((isMax || isMin) && scrubbedIndex != i) {
                val badgeText = if (isMax) "MAX" else "MIN"
                val ttLayout = textMeasurer.measure(badgeText, TextStyle(color = pointColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                drawText(textMeasurer, badgeText, topLeft = Offset(pt.x - ttLayout.size.width/2f, pt.y - 20.dp.toPx()), style = TextStyle(color = pointColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
            }
        }
        
        // Tooltip for scrubbing
        if (scrubbedIndex in data.indices) {
            val pt = points[scrubbedIndex]
            val dp = data[scrubbedIndex]
            
            drawLine(color = Color.Gray.copy(alpha = 0.5f), start = Offset(pt.x, 0f), end = Offset(pt.x, height), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            
            val tooltipText = "${dp.label}\n${dp.value.format(1)}h"
            val ttLayout = textMeasurer.measure(tooltipText, tooltipStyle)
            val tooltipX = (pt.x - ttLayout.size.width / 2f).coerceIn(0f, width - ttLayout.size.width)
            val tooltipY = (pt.y - 40.dp.toPx() - ttLayout.size.height).coerceAtLeast(0f)
            
            drawRoundRect(color = Color.DarkGray.copy(alpha=0.9f), topLeft = Offset(tooltipX - 8.dp.toPx(), tooltipY - 4.dp.toPx()), size = Size(ttLayout.size.width + 16.dp.toPx(), ttLayout.size.height + 8.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
            drawText(textMeasurer, tooltipText, topLeft = Offset(tooltipX, tooltipY), style = tooltipStyle)
        }
    }
}

@Composable
fun EnhancedPieChart(values: List<Float>, colors: List<Color>, labels: List<String>, modifier: Modifier = Modifier) {
    val total = values.sum().coerceAtLeast(0.01f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val sweepAngles = values.map { (it / total) * 360f }
                
                sweepAngles.forEachIndexed { i, sweep ->
                    val animatedSweep = sweep * animatedProgress
                    drawArc(
                        color = colors.getOrElse(i) { Color.Gray },
                        startAngle = startAngle,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        style = Stroke(width = 30.dp.toPx())
                    )
                    startAngle += animatedSweep
                }
            }
            Text("${total.format(1)}h", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEachIndexed { i, label ->
                if (values.getOrElse(i){0f} > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.getOrElse(i) { Color.Gray }))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
