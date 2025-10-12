package com.example.presentmate

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class PresentMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
    }
}
