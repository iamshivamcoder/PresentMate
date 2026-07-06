import re

with open('app/src/main/java/com/example/presentmate/db/PresentMateDatabase.kt', 'r') as f:
    content = f.read()

merged = """
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

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_records_userId_date` ON `attendance_records` (`userId`, `date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_records_userId` ON `deleted_records` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_session_logs_userId_calendarEventId` ON `study_session_logs` (`userId`, `calendarEventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_places_userId` ON `saved_places` (`userId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(\"\"\"
                    CREATE TABLE IF NOT EXISTS chat_sessions (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastMessageAt INTEGER NOT NULL
                    )
                \"\"\".trimIndent())
                db.execSQL(\"\"\"
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT PRIMARY KEY NOT NULL,
                        sessionId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        isFromUser INTEGER NOT NULL,
                        imageUriString TEXT,
                        createdAt INTEGER NOT NULL
                    )
                \"\"\".trimIndent())
            }
        }"""

new_content = re.sub(r'<<<<<<< HEAD.*?>>>>>>> [a-f0-9a-zA-Z ()]+', merged, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentmate/db/PresentMateDatabase.kt', 'w') as f:
    f.write(new_content)
