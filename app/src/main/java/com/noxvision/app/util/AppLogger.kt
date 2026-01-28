package com.noxvision.app.util

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    data class LogEntry(
        val timestamp: String,
        val message: String,
        val type: LogType
    )

    enum class LogType {
        INFO, SUCCESS, ERROR
    }

    private val logs = mutableStateListOf<LogEntry>()
    val logsList: List<LogEntry> get() = logs

    fun log(message: String, type: LogType = LogType.INFO) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, LogEntry(timestamp, message, type))
        if (logs.size > 100) {
            logs.removeAt(logs.size - 1)
        }
        Log.d("AppLogger", "[$type] $message")
    }

    fun clear() {
        logs.clear()
    }
}
