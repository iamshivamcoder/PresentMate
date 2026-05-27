package com.example.presentmate

import android.content.Context
import android.content.SharedPreferences

/**
 * Fix #20 — persists search history to SharedPreferences as an ordered list
 * (most recent first, capped at 30 entries) so history survives app kills.
 */
class SearchHistoryRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val KEY = "history_ordered"
    private val MAX_ENTRIES = 30

    /** Returns history in most-recent-first order. */
    fun getSearchHistory(): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return raw.split("\u0000").filter { it.isNotBlank() }
    }

    /** Adds a query to the top of the history list. Deduplicates automatically. */
    fun addToSearchHistory(query: String) {
        if (query.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.remove(query)        // remove duplicate if present
        current.add(0, query)        // most recent first
        val trimmed = current.take(MAX_ENTRIES)
        prefs.edit().putString(KEY, trimmed.joinToString("\u0000")).apply()
    }

    fun removeFromSearchHistory(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        prefs.edit().putString(KEY, current.joinToString("\u0000")).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY).apply()
    }
}
