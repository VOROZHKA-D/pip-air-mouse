package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class SensorTelemetry(
    val rawGyroX: Float = 0f,
    val rawGyroY: Float = 0f,
    val rawGyroZ: Float = 0f,
    val dx: Int = 0,
    val dy: Int = 0,
    val activeButtons: Int = 0,
    val isMoving: Boolean = false,
    val eventsCount: Long = 0L,
    val reportsSentCount: Long = 0L
)
