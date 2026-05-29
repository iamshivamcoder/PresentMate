package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.compose.runtime.Immutable

@Entity(tableName = "attendance_records", indices = [androidx.room.Index(value = ["userId", "date"])])
@Immutable
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val date: Long,
    val timeIn: Long? = null,
    val timeOut: Long? = null
)

@Entity(tableName = "deleted_records", indices = [androidx.room.Index(value = ["userId"])])
@Immutable
data class DeletedRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalId: Int,
    val userId: String = "",
    val date: Long,
    val timeIn: Long? = null,
    val timeOut: Long? = null,
    val deletedAt: Long = System.currentTimeMillis()
)
