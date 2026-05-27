package com.example.presentmate.di

import android.content.Context
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.StudySessionLogDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

import com.example.presentmate.db.StepActivityLogDao

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceBroadcastReceiverEntryPoint {

    fun applicationScope(): CoroutineScope
    fun appDatabase(): PresentMateDatabase
    fun attendanceDao(): AttendanceDao
    fun studySessionLogDao(): StudySessionLogDao
    fun stepActivityLogDao(): StepActivityLogDao
}

fun getEntryPoint(context: Context): GeofenceBroadcastReceiverEntryPoint {
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        GeofenceBroadcastReceiverEntryPoint::class.java
    )
}

