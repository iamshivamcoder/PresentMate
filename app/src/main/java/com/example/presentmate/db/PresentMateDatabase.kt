package com.example.presentmate.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.presentmate.data.SavedPlace
import com.example.presentmate.data.SavedPlaceDao
import kotlinx.coroutines.flow.Flow

// --- AttendanceDao --- //
@androidx.room.Dao
interface AttendanceDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @androidx.room.Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @androidx.room.Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecordsNonFlow(): List<AttendanceRecord>

    @androidx.room.Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: Long): AttendanceRecord?

    @androidx.room.Update
    suspend fun updateRecord(record: AttendanceRecord)

    @androidx.room.Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord)

    @androidx.room.Query("SELECT * FROM deleted_records ORDER BY deletedAt DESC")
    fun getAllDeletedRecords(): Flow<List<DeletedRecord>>

    @androidx.room.Query("DELETE FROM deleted_records WHERE id = :id")
    suspend fun permanentlyDeleteRecord(id: Int)

    @androidx.room.Query("SELECT * FROM attendance_records WHERE timeOut IS NULL ORDER BY timeIn DESC LIMIT 1")
    fun getOngoingSession(): AttendanceRecord?

    @androidx.room.Query("SELECT * FROM attendance_records WHERE timeOut IS NULL ORDER BY timeIn DESC LIMIT 1")
    fun getOngoingSessionFlow(): Flow<AttendanceRecord?>
}

// --- Unified Database --- //
@Database(
    entities = [AttendanceRecord::class, DeletedRecord::class, SavedPlace::class],
    version = 3,
    exportSchema = false
)
abstract class PresentMateDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: PresentMateDatabase? = null

        fun getDatabase(context: Context): PresentMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PresentMateDatabase::class.java,
                    "presentmate_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
