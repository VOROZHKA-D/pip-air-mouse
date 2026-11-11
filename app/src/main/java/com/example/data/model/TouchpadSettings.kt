package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class TouchpadSettings(
    val touchpadSensitivity: Float = 1.4f,      // Range: 0.5f..3.0f
    val pointerAcceleration: Boolean = true,    // Exponential curve for rapid flicks vs slow precision
    val scrollSensitivity: Float = 1.2f,        // Range: 0.5f..3.0f
    val naturalScrolling: Boolean = true,       // True = macOS style (swipe up scrolls down content)
    val tapToClick: Boolean = true,
    val threeFingerDrag: Boolean = true
)
