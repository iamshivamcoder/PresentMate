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
    version = 8,
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
                // Migrate attendance_records
                db.execSQL("CREATE TABLE IF NOT EXISTS attendance_records_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, date INTEGER NOT NULL, timeIn INTEGER, timeOut INTEGER)")
                db.execSQL("INSERT INTO attendance_records_new (id, userId, date, timeIn, timeOut) SELECT id, '', date, timeIn, timeOut FROM attendance_records")
                db.execSQL("DROP TABLE attendance_records")
                db.execSQL("ALTER TABLE attendance_records_new RENAME TO attendance_records")

                // Migrate deleted_records
                db.execSQL("CREATE TABLE IF NOT EXISTS deleted_records_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, originalId INTEGER NOT NULL, userId TEXT NOT NULL, date INTEGER NOT NULL, timeIn INTEGER, timeOut INTEGER, deletedAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO deleted_records_new (id, originalId, userId, date, timeIn, timeOut, deletedAt) SELECT id, originalId, '', date, timeIn, timeOut, deletedAt FROM deleted_records")
                db.execSQL("DROP TABLE deleted_records")
                db.execSQL("ALTER TABLE deleted_records_new RENAME TO deleted_records")

                // Migrate saved_places
                db.execSQL("CREATE TABLE IF NOT EXISTS saved_places_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, radius REAL NOT NULL)")
                // Check if radius column exists in the old table using PRAGMA before selecting
                val cursor = db.query("PRAGMA table_info(saved_places)")
                var hasRadius = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "radius") {
                        hasRadius = true
                        break
                    }
                }
                cursor.close()
                if (hasRadius) {
                    db.execSQL("INSERT INTO saved_places_new (id, userId, name, address, latitude, longitude, radius) SELECT id, '', name, address, latitude, longitude, radius FROM saved_places")
                } else {
                    db.execSQL("INSERT INTO saved_places_new (id, userId, name, address, latitude, longitude, radius) SELECT id, '', name, address, latitude, longitude, 100.0 FROM saved_places")
                }
                db.execSQL("DROP TABLE saved_places")
                db.execSQL("ALTER TABLE saved_places_new RENAME TO saved_places")

                // Migrate study_session_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS study_session_logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, calendarEventId INTEGER NOT NULL, eventTitle TEXT NOT NULL, subject TEXT, topic TEXT, scheduledStartTime INTEGER NOT NULL, scheduledEndTime INTEGER NOT NULL, status TEXT NOT NULL, actualDurationMinutes INTEGER, recallNote TEXT, loggedAt INTEGER)")
                db.execSQL("INSERT INTO study_session_logs_new (id, userId, calendarEventId, eventTitle, subject, topic, scheduledStartTime, scheduledEndTime, status, actualDurationMinutes, recallNote, loggedAt) SELECT id, '', calendarEventId, eventTitle, subject, topic, scheduledStartTime, scheduledEndTime, IFNULL(status, 'PENDING'), actualDurationMinutes, recallNote, loggedAt FROM study_session_logs")
                db.execSQL("DROP TABLE study_session_logs")
                db.execSQL("ALTER TABLE study_session_logs_new RENAME TO study_session_logs")

                // Migrate step_activity_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS step_activity_logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, detectedAt INTEGER NOT NULL, stepCount INTEGER NOT NULL, windowMinutes INTEGER NOT NULL, type TEXT NOT NULL, window TEXT NOT NULL, triggered INTEGER NOT NULL)")
                db.execSQL("INSERT INTO step_activity_logs_new (id, userId, detectedAt, stepCount, windowMinutes, type, window, triggered) SELECT id, '', detectedAt, IFNULL(stepCount, 0), IFNULL(windowMinutes, 30), IFNULL(type, 'PERIODIC_SYNC'), IFNULL(window, 'BACKGROUND'), IFNULL(triggered, 0) FROM step_activity_logs")
                db.execSQL("DROP TABLE step_activity_logs")
                db.execSQL("ALTER TABLE step_activity_logs_new RENAME TO step_activity_logs")
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Re-run the robust recreate-and-copy migration for any users who ended up with a broken schema on version 7

                // Migrate attendance_records
                db.execSQL("CREATE TABLE IF NOT EXISTS attendance_records_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, date INTEGER NOT NULL, timeIn INTEGER, timeOut INTEGER)")
                db.execSQL("INSERT INTO attendance_records_new (id, userId, date, timeIn, timeOut) SELECT id, IFNULL(userId, ''), date, timeIn, timeOut FROM attendance_records")
                db.execSQL("DROP TABLE attendance_records")
                db.execSQL("ALTER TABLE attendance_records_new RENAME TO attendance_records")

                // Migrate deleted_records
                db.execSQL("CREATE TABLE IF NOT EXISTS deleted_records_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, originalId INTEGER NOT NULL, userId TEXT NOT NULL, date INTEGER NOT NULL, timeIn INTEGER, timeOut INTEGER, deletedAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO deleted_records_new (id, originalId, userId, date, timeIn, timeOut, deletedAt) SELECT id, originalId, IFNULL(userId, ''), date, timeIn, timeOut, deletedAt FROM deleted_records")
                db.execSQL("DROP TABLE deleted_records")
                db.execSQL("ALTER TABLE deleted_records_new RENAME TO deleted_records")

                // Migrate saved_places
                db.execSQL("CREATE TABLE IF NOT EXISTS saved_places_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, radius REAL NOT NULL)")
                // Check if radius column exists
                val cursor = db.query("PRAGMA table_info(saved_places)")
                var hasRadius = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "radius") {
                        hasRadius = true
                        break
                    }
                }
                cursor.close()
                if (hasRadius) {
                    db.execSQL("INSERT INTO saved_places_new (id, userId, name, address, latitude, longitude, radius) SELECT id, IFNULL(userId, ''), name, address, latitude, longitude, radius FROM saved_places")
                } else {
                    db.execSQL("INSERT INTO saved_places_new (id, userId, name, address, latitude, longitude, radius) SELECT id, IFNULL(userId, ''), name, address, latitude, longitude, 100.0 FROM saved_places")
                }
                db.execSQL("DROP TABLE saved_places")
                db.execSQL("ALTER TABLE saved_places_new RENAME TO saved_places")

                // Migrate study_session_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS study_session_logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, calendarEventId INTEGER NOT NULL, eventTitle TEXT NOT NULL, subject TEXT, topic TEXT, scheduledStartTime INTEGER NOT NULL, scheduledEndTime INTEGER NOT NULL, status TEXT NOT NULL, actualDurationMinutes INTEGER, recallNote TEXT, loggedAt INTEGER)")
                db.execSQL("INSERT INTO study_session_logs_new (id, userId, calendarEventId, eventTitle, subject, topic, scheduledStartTime, scheduledEndTime, status, actualDurationMinutes, recallNote, loggedAt) SELECT id, IFNULL(userId, ''), calendarEventId, eventTitle, subject, topic, scheduledStartTime, scheduledEndTime, IFNULL(status, 'PENDING'), actualDurationMinutes, recallNote, loggedAt FROM study_session_logs")
                db.execSQL("DROP TABLE study_session_logs")
                db.execSQL("ALTER TABLE study_session_logs_new RENAME TO study_session_logs")

                // Migrate step_activity_logs
                db.execSQL("CREATE TABLE IF NOT EXISTS step_activity_logs_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId TEXT NOT NULL, detectedAt INTEGER NOT NULL, stepCount INTEGER NOT NULL, windowMinutes INTEGER NOT NULL, type TEXT NOT NULL, window TEXT NOT NULL, triggered INTEGER NOT NULL)")
                db.execSQL("INSERT INTO step_activity_logs_new (id, userId, detectedAt, stepCount, windowMinutes, type, window, triggered) SELECT id, IFNULL(userId, ''), detectedAt, IFNULL(stepCount, 0), IFNULL(windowMinutes, 30), IFNULL(type, 'PERIODIC_SYNC'), IFNULL(window, 'BACKGROUND'), IFNULL(triggered, 0) FROM step_activity_logs")
                db.execSQL("DROP TABLE step_activity_logs")
                db.execSQL("ALTER TABLE step_activity_logs_new RENAME TO step_activity_logs")
            }
        }

        fun getDatabase(context: Context): PresentMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PresentMateDatabase::class.java,
                    "presentmate_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
