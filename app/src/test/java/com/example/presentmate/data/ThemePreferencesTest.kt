package com.example.presentmate.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemePreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
    }

    @Test
    fun `init loads default value when not set`() {
        every { sharedPreferences.getString("theme_mode", "system") } returns "system"

        ThemePreferences.init(context)

        assertEquals("system", ThemePreferences.currentThemeState.value)
        verify { sharedPreferences.getString("theme_mode", "system") }
    }

    @Test
    fun `init loads saved value`() {
        every { sharedPreferences.getString("theme_mode", "system") } returns "dark"

        ThemePreferences.init(context)

        assertEquals("dark", ThemePreferences.currentThemeState.value)
    }

    @Test
    fun `setTheme saves value and updates state`() {
        every { sharedPreferences.getString("theme_mode", "system") } returns "system"
        ThemePreferences.init(context) // start state

        ThemePreferences.setTheme(context, "light")

        verify { editor.putString("theme_mode", "light") }
        verify { editor.apply() }
        assertEquals("light", ThemePreferences.currentThemeState.value)
    }
}
