package com.example.presentmate.services

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.presentmate.R // Added import for project resources
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class SessionTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(applicationContext)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        serviceScope.launch {
            try {
                val attendanceDao = db.attendanceDao()
                val ongoingSession = attendanceDao.getAllRecords().firstOrNull()?.lastOrNull { it.timeOut == null }

                if (ongoingSession != null) {
                    // End current session
                    attendanceDao.updateRecord(ongoingSession.copy(timeOut = System.currentTimeMillis()))
                } else {
                    // Start new session
                    val now = System.currentTimeMillis()
                    attendanceDao.insertRecord(AttendanceRecord(date = now, timeIn = now, timeOut = null))
                }
            } catch (e: Exception) {
                Log.e("SessionTileService", "Error onClick: ", e)
            }
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        // No specific cleanup needed for SupervisorJob with IO dispatcher for this use case
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        serviceScope.launch {
            try {
                val ongoingSession = db.attendanceDao().getAllRecords().firstOrNull()?.lastOrNull { it.timeOut == null }
                val isSessionActive = ongoingSession != null

                if (isSessionActive) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.icon = Icon.createWithResource(this@SessionTileService, R.drawable.ic_session)
                    tile.label = "End Session"
                } else {
                    tile.state = Tile.STATE_INACTIVE
                    tile.icon = Icon.createWithResource(this@SessionTileService, R.drawable.ic_session)
                    tile.label = "Start Session"
                }
            } catch (e: Exception) {
                 Log.e("SessionTileService", "Error updateTile: ", e)
                tile.state = Tile.STATE_UNAVAILABLE
                tile.label = "Error"
                 // Consider a more specific error icon if available
                tile.icon = Icon.createWithResource(this@SessionTileService, R.drawable.ic_session) 
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SupervisorJob cancels its children when the scope is cancelled, which happens implicitly if service is unbound and destroyed.
    }
}
