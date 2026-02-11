package com.example.presentmate.ui.screens

import android.content.Context
import android.content.SharedPreferences
import com.example.presentmate.data.GeofencePreferencesRepository
import com.example.presentmate.viewmodel.LocationViewModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationScreenTest {

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var sharedPreferences: SharedPreferences

    @MockK(relaxed = true)
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var viewModel: LocationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockkObject(GeofencePreferencesRepository)
        every { GeofencePreferencesRepository.getPreferences(any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial tracking state should be false when disabled in preferences`() = runTest(testDispatcher) {
        // Given
        every { GeofencePreferencesRepository.isGeofenceEnabled(any()) } returns false

        // When
        viewModel = LocationViewModel(context)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isTrackingEnabled.value)
    }

    @Test
    fun `initial tracking state should be true when enabled in preferences`() = runTest(testDispatcher) {
        // Given
        every { GeofencePreferencesRepository.isGeofenceEnabled(any()) } returns true

        // When
        viewModel = LocationViewModel(context)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isTrackingEnabled.value)
    }

    @Test
    fun `setTrackingEnabled should update tracking state to true`() = runTest(testDispatcher) {
        // Given
        every { GeofencePreferencesRepository.isGeofenceEnabled(any()) } returns false
        every { GeofencePreferencesRepository.setGeofenceEnabled(any(), any()) } returns Unit
        viewModel = LocationViewModel(context)
        advanceUntilIdle()

        // When
        viewModel.setTrackingEnabled(true)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.isTrackingEnabled.value)
        verify { GeofencePreferencesRepository.setGeofenceEnabled(any(), true) }
    }

    @Test
    fun `setTrackingEnabled should update tracking state to false`() = runTest(testDispatcher) {
        // Given
        every { GeofencePreferencesRepository.isGeofenceEnabled(any()) } returns true
        every { GeofencePreferencesRepository.setGeofenceEnabled(any(), any()) } returns Unit
        viewModel = LocationViewModel(context)
        advanceUntilIdle()

        // When
        viewModel.setTrackingEnabled(false)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.isTrackingEnabled.value)
        verify { GeofencePreferencesRepository.setGeofenceEnabled(any(), false) }
    }
}
