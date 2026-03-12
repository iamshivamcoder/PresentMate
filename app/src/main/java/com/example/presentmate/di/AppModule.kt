package com.example.presentmate.di

import android.content.Context
import com.example.presentmate.calendar.CalendarRepository
import com.example.presentmate.data.SavedPlaceDao
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StudySessionLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PresentMateDatabase {
        return PresentMateDatabase.getDatabase(context)
    }

    @Provides
    fun provideAttendanceDao(database: PresentMateDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    fun provideSavedPlaceDao(database: PresentMateDatabase): SavedPlaceDao {
        return database.savedPlaceDao()
    }
    
    @Provides
    fun provideStudySessionLogDao(database: PresentMateDatabase): StudySessionLogDao {
        return database.studySessionLogDao()
    }
    
    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): CalendarRepository {
        return CalendarRepository(context)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
