package com.example.presentmate.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit [TestWatcher] rule that automatically installs and removes a [TestDispatcher]
 * on [Dispatchers.Main] for each test.
 *
 * Usage:
 * ```
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 *
 * By default uses [UnconfinedTestDispatcher] so coroutines run eagerly without
 * needing explicit `advanceUntilIdle()` calls for simple cases. Pass
 * [StandardTestDispatcher] if you need to control scheduling manually.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
