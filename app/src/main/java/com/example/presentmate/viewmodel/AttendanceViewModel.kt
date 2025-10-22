package com.example.presentmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.AttendanceRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(private val attendanceDao: AttendanceDao) : ViewModel() {

    val ongoingSession: StateFlow<AttendanceRecord?> = attendanceDao.getOngoingSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRecords: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            attendanceDao.insertRecord(AttendanceRecord(date = now, timeIn = now, timeOut = null))
        }
    }

    fun endSession() {
        viewModelScope.launch {
            ongoingSession.value?.let {
                attendanceDao.updateRecord(it.copy(timeOut = System.currentTimeMillis()))
            }
        }
    }
}