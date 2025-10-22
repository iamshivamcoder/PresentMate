package com.example.presentmate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class PresentMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Use a named SharedPreferences to initialize osmdroid configuration
        val prefs = getSharedPreferences("presentmate_osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
    }
}
