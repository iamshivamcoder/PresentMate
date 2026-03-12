package com.example.presentmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.ui.components.GraphDataPoint
import com.example.presentmate.ui.components.GraphStats
import com.example.presentmate.ui.components.GraphViewType
import com.example.presentmate.ui.components.calculateGraphData
import com.example.presentmate.ui.screens.DailySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OverviewUiState(
    val dailySummaries: List<DailySummary> = emptyList(),
    val graphData: List<GraphDataPoint> = emptyList(),
    val stats: GraphStats = GraphStats(0f, 0f, "-", 0f),
    val selectedGraphViewType: GraphViewType = GraphViewType.WEEKLY,
    val currentDisplayDate: LocalDate = LocalDate.now()
)

@HiltViewModel
class OverviewViewModel @Inject constructor(private val attendanceDao: AttendanceDao) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceDao.getAllRecords().collect { records ->
                processRecords(records)
            }
        }
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(currentDisplayDate = newDate) }
        processRecords(attendanceDao.getAllRecordsNonFlow())
    }

    fun onViewTypeChange(newViewType: GraphViewType) {
        _uiState.update { it.copy(selectedGraphViewType = newViewType) }
        processRecords(attendanceDao.getAllRecordsNonFlow())
    }

    private fun processRecords(records: List<AttendanceRecord>) {
        viewModelScope.launch(Dispatchers.Default) {
            val dailySummaries = records
                .filter { it.timeIn != null && it.timeOut != null && it.timeOut > it.timeIn }
                .groupBy { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() }
                .map { (date, recordsOnDate) ->
                    val totalDuration = recordsOnDate.sumOf { 
                        if (it.timeOut != null && it.timeIn != null) {
                            it.timeOut - it.timeIn
                        } else 0L
                    }
                    DailySummary(date, totalDuration, recordsOnDate)
                }
                .sortedByDescending { it.date }

            val graphData = calculateGraphData(records, _uiState.value.selectedGraphViewType, _uiState.value.currentDisplayDate)

            val totalHours = graphData.map { it.value }.sum()
            val averageHours = if (graphData.isNotEmpty()) totalHours / graphData.size else 0f
            val bestDay = graphData.maxByOrNull { it.value }?.label ?: "-"
            val goalProgress = totalHours // You can adjust this if you have a goal value
            val stats = GraphStats(totalHours, averageHours, bestDay, goalProgress)

            _uiState.update {
                it.copy(
                    dailySummaries = dailySummaries,
                    graphData = graphData,
                    stats = stats
                )
            }
        }
    }
}
