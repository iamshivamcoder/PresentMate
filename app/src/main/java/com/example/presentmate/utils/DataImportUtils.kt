package com.example.presentmate.utils

import android.content.Context
import com.example.presentmate.db.AttendanceRecord
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.Range
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

object DataImportUtils {

    fun importAttendanceDataFromDoc(context: Context, fileName: String): List<AttendanceRecord> {
        val records = mutableListOf<AttendanceRecord>()
        val file = File(context.getExternalFilesDir(null), fileName)

        if (!file.exists()) {
            // Consider logging or throwing an exception
            return emptyList()
        }

        try {
            FileInputStream(file).use { inputStream ->
                val document = HWPFDocument(inputStream)
                val range: Range = document.getRange()
                val text = range.text()
                val lines = text.split("\r") // HWPF uses \r for paragraph breaks

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                // Skip header lines - adjust if header format changes in export
                // 1. Title: "Attendance Records Backup"
                // 2. Empty line
                // 3. Header: "Date\t\tTime In\t\tTime Out\t\tDuration (min)"
                // 4. Separator: "----------------------------------------------------------------------"
                val dataLines = lines.dropWhile { it.isBlank() || it.startsWith("Attendance Records Backup") || it.startsWith("Date") || it.startsWith("----") }

                for (line in dataLines) {
                    if (line.isBlank() || line.startsWith("End of Report")) {
                        continue // Skip empty lines or footer
                    }

                    val parts = line.split("\t")
                    // Expected format: Date, Time In, Time Out, (empty tab), Duration
                    // Due to \t\t in export for duration, there might be an empty part[3]
                    if (parts.size >= 4) { // Basic check for enough parts
                        val dateStr = parts[0]
                        val timeInStr = parts[1]
                        val timeOutStr = parts[2]
                        // Duration (parts[3] or parts[4] if double tab used correctly) is not directly needed to reconstruct AttendanceRecord's core fields

                        try {
                            val date = sdfDate.parse(dateStr)?.time ?: continue // Skip if date is invalid

                            val timeIn = if (timeInStr != "N/A") sdfTime.parse(timeInStr)?.time else null
                            val timeOut = if (timeOutStr != "N/A" && timeOutStr != "Ongoing") sdfTime.parse(timeOutStr)?.time else null

                            // Basic validation: timeIn must exist if timeOut exists, though export logic allows timeIn=null (resulting in N/A duration)
                            // However, an AttendanceRecord fundamentally needs a date. timeIn and timeOut can be null.
                            
                            records.add(AttendanceRecord(date = date, timeIn = timeIn, timeOut = timeOut))
                        } catch (e: Exception) {
                            e.printStackTrace() // Log parsing error for a specific line
                            // Continue to next line
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Log error opening or reading the file
            return emptyList() // Return empty or throw custom exception
        }
        return records
    }
}
