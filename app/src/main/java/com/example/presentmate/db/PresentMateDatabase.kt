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

    @androidx.room.Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecords(userId: String): Flow<List<AttendanceRecord>>

    @androidx.room.Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecordsNonFlow(userId: String): List<AttendanceRecord>

    @androidx.room.Query("SELECT * FROM attendance_records WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getRecordByDate(date: Long, userId: String): AttendanceRecord?

    @androidx.room.Update
    suspend fun updateRecord(record: AttendanceRecord)

    @androidx.room.Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord)

    @androidx.room.Query("SELECT * FROM deleted_records WHERE userId = :userId ORDER BY deletedAt DESC")
    fun getAllDeletedRecords(userId: String): Flow<List<DeletedRecord>>

    @androidx.room.Query("DELETE FROM deleted_records WHERE id = :id AND userId = :userId")
    suspend fun permanentlyDeleteRecord(id: Int, userId: String)

    @androidx.room.Query("SELECT * FROM attendance_records WHERE timeOut IS NULL AND userId = :userId ORDER BY timeIn DESC LIMIT 1")
    fun getOngoingSession(userId: String): AttendanceRecord?

    @androidx.room.Query("SELECT * FROM attendance_records WHERE timeOut IS NULL AND userId = :userId ORDER BY timeIn DESC LIMIT 1")
    fun getOngoingSessionFlow(userId: String): Flow<AttendanceRecord?>
}

// --- Unified Database --- //
@Database(
    entities = [AttendanceRecord::class, DeletedRecord::class, SavedPlace::class, StudySessionLog::class, StepActivityLog::class, ActivityEvent::class],
    version = 7,
    exportSchema = false
)
abstract class PresentMateDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun studySessionLogDao(): StudySessionLogDao
    abstract fun stepActivityLogDao(): StepActivityLogDao
    abstract fun activityEventDao(): ActivityEventDao

    companion object {
        @Volatile
        private var INSTANCE: PresentMateDatabase? = null
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS step_activity_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        stepCount INTEGER NOT NULL DEFAULT 0,
                        windowMinutes INTEGER NOT NULL DEFAULT 30,
                        type TEXT NOT NULL DEFAULT 'PERIODIC_SYNC',
                        window TEXT NOT NULL DEFAULT 'BACKGROUND',
                        triggered INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE deleted_records ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                
                // Add missing radius column before adding userId to avoid schema validation errors
                // This handles cases where radius was added to the data class without a proper earlier migration
                db.execSQL("ALTER TABLE saved_places ADD COLUMN radius REAL NOT NULL DEFAULT 100.0")
                db.execSQL("ALTER TABLE saved_places ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                
                db.execSQL("ALTER TABLE study_session_logs ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE step_activity_logs ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_events (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        metadataJson TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
