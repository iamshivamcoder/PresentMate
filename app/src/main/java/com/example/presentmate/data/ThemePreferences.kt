package com.example.presentmate.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object ThemePreferences {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    const val MODE_SYSTEM = "system"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"

    var currentThemeState = mutableStateOf(MODE_SYSTEM)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentThemeState.value = prefs.getString(KEY_THEME, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setTheme(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode).apply()
        currentThemeState.value = mode
    }
}
