package com.example.presentmate.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "DriveSyncManager"
    private val DB_NAME = "presentmate_database"
    private val ZIP_FILE_NAME = "presentmate_backup.zip"

    private fun getDriveService(): Drive {
        val userEmail = firebaseAuth.currentUser?.email
            ?: throw IllegalStateException("User must be logged in to sync with Drive")

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccountName = userEmail

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("PresentMate")
        .build()
    }

    suspend fun backupDatabaseToDrive(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            
            // 1. Zip the database files
            val zipFile = createDatabaseZip()
            if (!zipFile.exists()) return@withContext Result.failure(Exception("Failed to zip DB"))

            // 2. Check if backup already exists in appDataFolder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$ZIP_FILE_NAME'")
                .setFields("files(id, name)")
                .execute()

            val fileContent = FileContent("application/zip", zipFile)

            if (fileList.files.isEmpty()) {
                // Create new
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = ZIP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, fileContent).execute()
                Log.d(TAG, "Created new backup in Drive")
            } else {
                // Update existing
                val existingFileId = fileList.files[0].id
                driveService.files().update(existingFileId, null, fileContent).execute()
                Log.d(TAG, "Updated existing backup in Drive")
            }

            // Cleanup local zip
            zipFile.delete()
            Result.success(Unit)

        } catch (e: UserRecoverableAuthIOException) {
            // This happens if the user hasn't granted the Drive AppData permission yet.
            // The UI must catch this and launch e.intent
            Log.w(TAG, "User interaction required for Drive scopes", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup to Drive", e)
            Result.failure(e)
        }
    }

    suspend fun restoreDatabaseFromDrive(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()

            // 1. Find the backup
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$ZIP_FILE_NAME'")
                .setFields("files(id, name)")
                .execute()

            if (fileList.files.isEmpty()) {
                Log.d(TAG, "No backup found in Drive")
                return@withContext Result.success(false)
            }

            val fileId = fileList.files[0].id
            val zipFile = File(context.cacheDir, ZIP_FILE_NAME)

            // 2. Download the backup
            FileOutputStream(zipFile).use { outStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outStream)
            }

            // 3. Unzip to temporary backup database location
            val backupDbName = "presentmate_temp_backup.db"
            extractDatabaseZip(zipFile, backupDbName)
            zipFile.delete()

            // 4. Safely merge/replace data using Room to prevent SQLite corruption
            // We instantiate the backup database. Room will handle any schema migrations required!
            val backupDb = androidx.room.Room.databaseBuilder(
                context,
                com.example.presentmate.db.PresentMateDatabase::class.java,
                backupDbName
            ).build()

            val currentDb = com.example.presentmate.db.PresentMateDatabase.getDatabase(context)

            // Perform a safe data-level replace
            currentDb.withTransaction {
                currentDb.clearAllTables()
                
                // Copy AttendanceRecords
                val records = backupDb.attendanceDao().getAllRecordsNonFlow()
                records.forEach { currentDb.attendanceDao().insertRecord(it) }
                
                // Copy SavedPlaces
                val places = backupDb.savedPlaceDao().getAllNonFlow()
                places.forEach { currentDb.savedPlaceDao().insert(it) }
                
                // Copy StudySessions
                val sessions = backupDb.studySessionLogDao().getAllNonFlow()
                sessions.forEach { currentDb.studySessionLogDao().insert(it) }
                
                // Copy StepLogs
                val steps = backupDb.stepActivityLogDao().getAll()
                steps.forEach { currentDb.stepActivityLogDao().insert(it) }
            }

            // Close and delete backup DB
            backupDb.close()
            context.deleteDatabase(backupDbName)

            Log.d(TAG, "Successfully restored database safely via Room")
            Result.success(true)

        } catch (e: UserRecoverableAuthIOException) {
            Log.w(TAG, "User interaction required for Drive scopes", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from Drive", e)
            Result.failure(e)
        }
    }

    private fun createDatabaseZip(): File {
        val dbFile = context.getDatabasePath(DB_NAME)
        val walFile = context.getDatabasePath("$DB_NAME-wal")
        val shmFile = context.getDatabasePath("$DB_NAME-shm")

        val zipFile = File(context.cacheDir, ZIP_FILE_NAME)
        
        ZipOutputStream(FileOutputStream(zipFile)).use { zout ->
            // Add DB files
            val filesToZip = listOf(dbFile, walFile, shmFile)
            for (file in filesToZip) {
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry(file.name)
                        zout.putNextEntry(entry)
                        fis.copyTo(zout)
                        zout.closeEntry()
                    }
                }
            }
            
            // Add manifest.json
            val manifestJson = """
                {
                    "schemaVersion": 7,
                    "userId": "${firebaseAuth.currentUser?.uid ?: ""}",
                    "timestamp": ${System.currentTimeMillis()},
                    "appVersion": "${context.packageManager.getPackageInfo(context.packageName, 0).versionName}"
                }
            """.trimIndent()
            
            zout.putNextEntry(ZipEntry("manifest.json"))
            zout.write(manifestJson.toByteArray(Charsets.UTF_8))
            zout.closeEntry()
        }
        return zipFile
    }

    private fun extractDatabaseZip(zipFile: File, backupDbName: String) {
        val dbDir = context.getDatabasePath(backupDbName).parentFile ?: return
        if (!dbDir.exists()) dbDir.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // If it's a manifest, skip or parse
                if (entry.name == "manifest.json") {
                    entry = zis.nextEntry
                    continue
                }

                // Convert original name "presentmate_database-wal" -> "presentmate_temp_backup.db-wal"
                val suffix = if (entry.name.contains("-")) entry.name.substring(entry.name.indexOf("-")) else ""
                val targetName = backupDbName + suffix
                
                val outFile = File(dbDir, targetName)
                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }
                entry = zis.nextEntry
            }
        }
    }
}
