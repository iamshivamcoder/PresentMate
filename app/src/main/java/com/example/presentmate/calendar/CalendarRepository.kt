package com.example.presentmate.calendar

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val calendarId: Long
)

class CalendarRepository(private val context: Context) {

    /**
     * Check if READ_CALENDAR permission is granted
     */
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCalendarList(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            Log.w("CalendarRepository", "READ_CALENDAR permission not granted. Cannot fetch calendars.")
            return@withContext emptyList()
        }

        val calendarList = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )
        
        // Filter for primary calendars or those that are visible/synced if desired
        // For now, getting all calendars the user can see
        val selection = null 
        val selectionArgs = null

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                val idCol = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountCol = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Unknown"
                    val account = it.getString(accountCol) ?: ""
                    
                    calendarList.add(CalendarInfo(id, name, account))
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error querying calendars", e)
        }
        
        return@withContext calendarList
    }

    @SuppressLint("MissingPermission")
    suspend fun getTodayEvents(calendarId: Long? = null): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            Log.w("CalendarRepository", "READ_CALENDAR permission not granted. Cannot fetch events.")
            return@withContext emptyList()
        }
        
        val eventList = mutableListOf<CalendarEvent>()
        
        // Define "today" range
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID
        )

        // Construct selection
        // Instances table is queried by range uri, but we can also filter columns
        var selection = "${CalendarContract.Instances.END} >= ? AND ${CalendarContract.Instances.BEGIN} <= ?"
        var selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        
        if (calendarId != null && calendarId != -1L) {
            selection += " AND ${CalendarContract.Instances.CALENDAR_ID} = ?"
            selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString(), calendarId.toString())
        }

        // Build the URI for the instance range
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startOfDay)
        ContentUris.appendId(builder, endOfDay)

        try {
            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleCol = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginCol = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endCol = it.getColumnIndex(CalendarContract.Instances.END)
                val calIdCol = it.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "No Title"
                    val begin = it.getLong(beginCol)
                    val end = it.getLong(endCol)
                    val calId = it.getLong(calIdCol)
                    
                    eventList.add(CalendarEvent(id, title, begin, end, calId))
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error querying events", e)
        }

        return@withContext eventList
    }
}
