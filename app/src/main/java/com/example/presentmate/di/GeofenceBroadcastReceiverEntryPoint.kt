package com.example.presentmate.di

import android.content.Context
import com.example.presentmate.PresentMateApplication
import com.example.presentmate.db.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceBroadcastReceiverEntryPoint {

    fun applicationScope(): CoroutineScope
    fun appDatabase(): AppDatabase
}

fun getEntryPoint(context: Context): GeofenceBroadcastReceiverEntryPoint {
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        GeofenceBroadcastReceiverEntryPoint::class.java
    )
}
