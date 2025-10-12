package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val timeIn: Long? = null,
    val timeOut: Long? = null
)

@Entity(tableName = "deleted_records")
data class DeletedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalId: Int,
    val date: Long,
    val timeIn: Long? = null,
    val timeOut: Long? = null,
    val deletedAt: Long = System.currentTimeMillis()
)
