package com.example.presentmate.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.example.presentmate",
            profileBlock = {
                // Starts the app's main activity and waits for the first frame
                startActivityAndWait()
                // You could add interactions here if needed (e.g., clicking on buttons)
            }
        )
    }
}