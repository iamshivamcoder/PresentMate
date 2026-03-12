package com.example.presentmate.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.presentmate.data.SavedPlace
import com.example.presentmate.data.SavedPlaceDao
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    entities = [AttendanceRecord::class, DeletedRecord::class, SavedPlace::class, StudySessionLog::class],
    version = 4,
    exportSchema = false
)
abstract class PresentMateDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun studySessionLogDao(): StudySessionLogDao

    companion object {
        @Volatile
        private var INSTANCE: PresentMateDatabase? = null
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new study_session_logs table (additive change, no data loss)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS study_session_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        calendarEventId INTEGER NOT NULL,
                        eventTitle TEXT NOT NULL,
                        subject TEXT,
                        topic TEXT,
                        scheduledStartTime INTEGER NOT NULL,
                        scheduledEndTime INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        actualDurationMinutes INTEGER,
                        recallNote TEXT,
                        loggedAt INTEGER
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): PresentMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PresentMateDatabase::class.java,
                    "presentmate_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
