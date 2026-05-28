package com.example.presentmate.di

import android.content.Context
import com.example.presentmate.ai.AIPreferences
import com.example.presentmate.ai.AIService
import com.example.presentmate.ai.AIServiceFactory
import com.example.presentmate.calendar.CalendarRepository
import com.example.presentmate.data.SavedPlaceDao
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StepActivityLogDao
import com.example.presentmate.db.StudySessionLogDao
import com.google.firebase.auth.FirebaseAuth
import androidx.credentials.CredentialManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
    fun provideStepActivityLogDao(database: PresentMateDatabase): StepActivityLogDao {
        return database.stepActivityLogDao()
    }

    @Provides
    fun provideActivityEventDao(database: PresentMateDatabase): com.example.presentmate.db.ActivityEventDao {
        return database.activityEventDao()
    }

    @Provides
    @Singleton
    fun provideCalendarRepository(@ApplicationContext context: Context): CalendarRepository {
        return CalendarRepository(context)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides the currently configured AIService, or null if no API key is set.
     * Re-read on every ViewModel creation so settings changes are picked up.
     */
    @Provides
    fun provideAIService(@ApplicationContext context: Context): AIService? {
        val platform    = AIPreferences.getPlatform(context)
        val apiKey      = AIPreferences.getApiKey(context)
        val temperature = AIPreferences.getTemperature(context)
        val maxTokens   = AIPreferences.getMaxTokens(context)
        return AIServiceFactory.create(platform, apiKey, temperature, maxTokens)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }
}

