package com.example.presentmate.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.util.Date

// --- Entity --- //
@androidx.room.Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // Store date as Long (timestamp)
    val timeIn: Long? = null, // Store time as Long (timestamp)
    val timeOut: Long? = null // Store time as Long (timestamp)
)

// --- DAO --- //
@androidx.room.Dao
interface AttendanceDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @androidx.room.Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): kotlinx.coroutines.flow.Flow<List<AttendanceRecord>>

    @androidx.room.Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: Long): AttendanceRecord?

    @androidx.room.Update
    suspend fun updateRecord(record: AttendanceRecord)
}

// --- Database --- //
@Database(entities = [AttendanceRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}