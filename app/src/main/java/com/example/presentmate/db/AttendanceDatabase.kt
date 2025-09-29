package com.example.presentmate.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- DAO --- //
@androidx.room.Dao
interface AttendanceDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @androidx.room.Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): kotlinx.coroutines.flow.Flow<List<AttendanceRecord>>

    @androidx.room.Query("SELECT * FROM attendance_records ORDER BY date DESC") // Added for synchronous export
    fun getAllRecordsNonFlow(): List<AttendanceRecord> // Added for synchronous export

    @androidx.room.Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: Long): AttendanceRecord?

    @androidx.room.Update
    suspend fun updateRecord(record: AttendanceRecord)

    @androidx.room.Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord)

    @androidx.room.Query("SELECT * FROM deleted_records ORDER BY deletedAt DESC")
    fun getAllDeletedRecords(): kotlinx.coroutines.flow.Flow<List<DeletedRecord>>

    @androidx.room.Query("DELETE FROM deleted_records WHERE id = :id")
    suspend fun permanentlyDeleteRecord(id: Int)
}

// --- Database --- //
@Database(
    entities = [AttendanceRecord::class, DeletedRecord::class],
    version = 2, // Consider incrementing version if schema changed and no migration provided for new table/column, though just adding a query method is fine.
    exportSchema = false
)
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
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
