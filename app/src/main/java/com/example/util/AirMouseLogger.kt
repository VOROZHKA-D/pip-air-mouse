package com.example.util

import android.util.Log
import com.example.data.model.LogEntry
import com.example.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory thread-safe logger for Pip Air Mouse.
 * Exposes logs as StateFlow for UI display and writes to Logcat.
 */
object AirMouseLogger {
    private const val TAG = "PipAirMouse"
    private const val MAX_LOGS = 250

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun i(message: String) = log(LogLevel.INFO, message)
    fun d(message: String) = log(LogLevel.DEBUG, message)
    fun w(message: String) = log(LogLevel.WARN, message)
    fun e(message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message: ${throwable.message}" else message
        log(LogLevel.ERROR, fullMsg)
        try {
            Log.e(TAG, message, throwable)
        } catch (_: RuntimeException) {}
    }

    private fun log(level: LogLevel, message: String) {
        try {
            when (level) {
                LogLevel.INFO -> Log.i(TAG, message)
                LogLevel.DEBUG -> Log.d(TAG, message)
                LogLevel.WARN -> Log.w(TAG, message)
                LogLevel.ERROR -> Log.e(TAG, message)
            }
        } catch (_: RuntimeException) {
            // Safe fallback during JVM unit tests
        }

        val entry = LogEntry(level = level, message = message)
        _logs.update { current ->
            if (current.size >= MAX_LOGS) {
                current.drop(current.size - MAX_LOGS + 1) + entry
            } else {
                current + entry
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
