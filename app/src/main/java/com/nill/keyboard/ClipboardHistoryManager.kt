package com.nill.keyboard

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class ClipboardHistoryManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nk_clipboard", Context.MODE_PRIVATE)
    private val KEY = "clips"
    private val MAX_ENTRIES = 50

    fun addEntry(text: String) {
        if (text.isBlank() || text.length > 5000) return
        val list = getHistory().toMutableList()
        list.remove(text) // Remove duplicate if exists
        list.add(0, text) // Add to top
        if (list.size > MAX_ENTRIES) {
            list.subList(MAX_ENTRIES, list.size).clear()
        }
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }
}
