package com.example.presentmate.utils

import android.content.Context
import android.net.Uri
import com.example.presentmate.db.AttendanceRecord
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles import/export of attendance data using CSV format
 * via Android's Storage Access Framework (SAF)
 */
object DataTransferManager {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val exportDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
    
    /**
     * Generates a timestamped filename for export
     */
    fun generateExportFileName(): String {
        return "presentmate_backup_${exportDateFormat.format(Date())}.csv"
    }
    
    /**
     * Exports attendance records to CSV format at the specified URI
     * 
     * @param context Application context
     * @param uri Target URI obtained from SAF file picker
     * @param records List of attendance records to export
     * @return Result with success status or error message
     */
    suspend fun exportToCSV(
        context: Context,
        uri: Uri,
        records: List<AttendanceRecord>
    ): ExportResult {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Write CSV header
                    writer.write("ID,Date,TimeIn,TimeOut,DurationMinutes\n")
                    
                    for (record in records) {
                        val dateStr = dateFormat.format(Date(record.date))
                        val timeInStr = record.timeIn?.let { timeFormat.format(Date(it)) } ?: ""
                        val timeOutStr = record.timeOut?.let { timeFormat.format(Date(it)) } ?: ""
                        
                        val durationMinutes = if (record.timeIn != null && record.timeOut != null) {
                            (record.timeOut - record.timeIn) / (1000 * 60)
                        } else {
                            0L
                        }
                        
                        writer.write("${record.id},$dateStr,$timeInStr,$timeOutStr,$durationMinutes\n")
                    }
                }
            } ?: return ExportResult.Error("Could not open output stream")
            
            ExportResult.Success(records.size)
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Imports attendance records from a CSV file
     * 
     * @param context Application context
     * @param uri Source URI obtained from SAF file picker
     * @return Result with parsed records or error message
     */
    suspend fun importFromCSV(
        context: Context,
        uri: Uri
    ): ImportResult {
        return try {
            val records = mutableListOf<AttendanceRecord>()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header line
                    val header = reader.readLine()
                    if (header == null || !header.startsWith("ID") && !header.contains("Date")) {
                        return ImportResult.Error("Invalid CSV format: missing header")
                    }
                    
                    var lineNumber = 1
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        val parts = line!!.split(",")
                        
                        if (parts.size < 4) {
                            continue // Skip malformed lines
                        }
                        
                        try {
                            // Parse date (column 1)
                            val date = dateFormat.parse(parts[1].trim())?.time
                                ?: continue
                            
                            // Parse timeIn (column 2) - may be empty
                            val timeIn = if (parts[2].trim().isNotEmpty()) {
                                timeFormat.parse(parts[2].trim())?.time
                            } else null
                            
                            // Parse timeOut (column 3) - may be empty
                            val timeOut = if (parts[3].trim().isNotEmpty()) {
                                timeFormat.parse(parts[3].trim())?.time
                            } else null
                            
                            records.add(
                                AttendanceRecord(
                                    date = date,
                                    timeIn = timeIn,
                                    timeOut = timeOut
                                )
                            )
                        } catch (e: Exception) {
                            // Skip lines that can't be parsed
                            continue
                        }
                    }
                }
            } ?: return ImportResult.Error("Could not open input stream")
            
            if (records.isEmpty()) {
                ImportResult.Error("No valid records found in file")
            } else {
                ImportResult.Success(records)
            }
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    sealed class ExportResult {
        data class Success(val recordCount: Int) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }
    
    sealed class ImportResult {
        data class Success(val records: List<AttendanceRecord>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
