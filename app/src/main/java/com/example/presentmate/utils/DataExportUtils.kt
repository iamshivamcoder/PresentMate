package com.example.presentmate.utils

import android.content.Context
import com.example.presentmate.db.AttendanceRecord
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.Range
import java.io.File
import java.io.FileNotFoundException // Added for specific exception handling
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportUtils {

    fun exportAttendanceDataToDoc(context: Context, records: List<AttendanceRecord>, fileName: String): Boolean {
        if (records.isEmpty()) {
            return false
        }

        val assetManager = context.assets
        val inputStream: InputStream

        try {
            // Check if template.doc exists before trying to use it
            try {
                inputStream = assetManager.open("template.doc")
            } catch (e: FileNotFoundException) {
                System.err.println("Error: template.doc not found in assets. Please add a valid Word 97-2003 template.")
                e.printStackTrace() // Print stack trace for more details in logcat
                return false // Indicate export failure
            }

            val document = HWPFDocument(inputStream)
            val range: Range = document.getRange()

            range.insertAfter("Attendance Records Backup\r\r")

            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            range.insertAfter("Date\t\tTime In\t\tTime Out\t\tDuration (min)\r")
            range.insertAfter("----------------------------------------------------------------------\r")

            for (record in records) {
                val dateStr = sdfDate.format(Date(record.date))
                val timeInStr = record.timeIn?.let { sdfTime.format(Date(it)) } ?: "N/A"
                val timeOutStr = record.timeOut?.let { sdfTime.format(Date(it)) } ?: "N/A"

                var durationStr = "N/A"
                if (record.timeIn != null) {
                    if (record.timeOut != null) {
                        val durationMillis = record.timeOut - record.timeIn
                        val durationMinutes = durationMillis / (1000 * 60)
                        durationStr = durationMinutes.toString()
                    } else {
                        durationStr = "Ongoing"
                    }
                }

                val recordLine = "$dateStr\t$timeInStr\t$timeOutStr\t\t$durationStr\r"
                range.insertAfter(recordLine)
            }

            range.insertAfter("\rEnd of Report\r")

            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { outputStream ->
                document.write(outputStream)
            }
            inputStream.close()
            return true
        } catch (e: Exception) { // Catch other potential exceptions during POI operations
            System.err.println("Error during document processing: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}
