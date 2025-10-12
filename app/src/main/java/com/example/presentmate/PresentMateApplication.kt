package com.example.presentmate

import android.app.Application
import org.osmdroid.config.Configuration

class PresentMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
    }
}
