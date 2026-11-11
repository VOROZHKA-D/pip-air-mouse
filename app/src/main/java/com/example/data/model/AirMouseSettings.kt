package com.example.data.model

import androidx.compose.runtime.Immutable

enum class GripMode(val titleRu: String, val descRu: String) {
    POINTER("Как пульт ДУ", "Телефон направлен вперед на экран (указка)"),
    PORTRAIT("Вертикально", "Телефон удерживается вертикально в руке")
}

@Immutable
data class AirMouseSettings(
    val sensitivity: Float = 1.2f,        // 0.5x to 2.5x
    val deadzone: Float = 0.04f,          // rad/s (default 0.04 rad/s)
    val smoothingAlpha: Float = 0.35f,    // EMA alpha (0.1 to 0.9)
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val gripMode: GripMode = GripMode.POINTER,
    val hapticFeedback: Boolean = true,
    val testMode: Boolean = false
)
