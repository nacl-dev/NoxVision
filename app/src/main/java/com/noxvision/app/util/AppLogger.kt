package com.noxvision.app.util

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.noxvision.app.BuildConfig
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

    val logsList: List<LogEntry>
        field = mutableStateListOf<LogEntry>()

    fun log(message: String, type: LogType = LogType.INFO) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logsList.add(0, LogEntry(timestamp, message, type))
        if (logsList.size > 100) {
            logsList.removeAt(logsList.size - 1)
        }
        if (BuildConfig.DEBUG) {
            Log.d("AppLogger", "[$type] $message")
        }
    }

    fun clear() {
        logsList.clear()
    }
}
