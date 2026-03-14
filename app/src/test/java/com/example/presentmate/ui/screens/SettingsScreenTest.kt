package com.example.presentmate.ui.screens

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.presentmate.db.AttendanceDao
import com.example.presentmate.db.DeletedRecord
import com.example.presentmate.utils.DataTransferManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var packageManager: PackageManager

    @MockK
    private lateinit var attendanceDao: AttendanceDao

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        mockkObject(DataTransferManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getAppVersion returns correct version when package info available`() {
        // Given
        val expectedVersion = "1.2.3"
        val packageInfo = PackageInfo().apply { versionName = expectedVersion }
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.example.presentmate"
        every { packageManager.getPackageInfo("com.example.presentmate", 0) } returns packageInfo

        // When
        val version = getAppVersion(context)

        // Then
        assertEquals(expectedVersion, version)
    }

    @Test
    fun `getAppVersion returns NA when exception occurs`() {
        // Given
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.example.presentmate"
        every { packageManager.getPackageInfo("com.example.presentmate", 0) } throws PackageManager.NameNotFoundException()

        // When
        val version = getAppVersion(context)

        // Then
        assertEquals("N/A", version)
    }

    @Test
    fun `export filename should be generated with correct format`() = runTest(testDispatcher) {
        // When
        val filename = DataTransferManager.generateExportFileName()
        advanceUntilIdle()

        // Then
        assertNotNull(filename)
        assertTrue(filename.contains("presentmate_backup_"))
        assertTrue(filename.endsWith(".csv"))
    }

    @Test
    fun `deleted records flow returns correct count`() = runTest(testDispatcher) {
        // Given
        val deletedRecords = listOf(
            DeletedRecord(id = 1, originalId = 10, date = 1000L, timeIn = 1000L, timeOut = 2000L),
            DeletedRecord(id = 2, originalId = 20, date = 2000L, timeIn = 2000L, timeOut = 3000L),
            DeletedRecord(id = 3, originalId = 30, date = 3000L, timeIn = 3000L, timeOut = 4000L)
        )
        every { attendanceDao.getAllDeletedRecords() } returns flowOf(deletedRecords)

        // When
        var recordCount = 0
        attendanceDao.getAllDeletedRecords().collect { records ->
            recordCount = records.size
        }
        advanceUntilIdle()

        // Then
        assertEquals(3, recordCount)
    }
}
