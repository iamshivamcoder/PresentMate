package com.example.presentmate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import javax.inject.Inject

@HiltAndroidApp
class PresentMateApplication : Application(), WorkConfiguration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Use a named SharedPreferences to initialize osmdroid configuration
        val prefs = getSharedPreferences("presentmate_osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
    }
}
