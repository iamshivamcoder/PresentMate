package com.example.presentmate

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    fun getSearchHistory(): List<String> {
        return prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
    }

    fun addToSearchHistory(query: String) {
        val history = getSearchHistory().toMutableSet()
        history.add(query)
        prefs.edit().putStringSet("history", history).apply()
    }

    fun removeFromSearchHistory(query: String) {
        val history = getSearchHistory().toMutableSet()
        history.remove(query)
        prefs.edit().putStringSet("history", history).apply()
    }
}
