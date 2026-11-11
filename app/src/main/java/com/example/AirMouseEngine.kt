package com.example

import kotlin.math.abs

/**
 * Sensor fusion engine converting raw Gyroscope readings (rad/s)
 * into smooth, responsive relative mouse displacement (dx, dy).
 */
class AirMouseEngine(
    var sensitivity: Float = 45.0f,     // Range: 15.0..80.0, default 45.0
    var deadzone: Float = 0.04f,        // rad/s (ignores micro hand tremors)
    var alpha: Float = 0.25f,           // EMA low-pass filter factor
    var invertX: Boolean = false,
    var invertY: Boolean = false,
    var usePointerMode: Boolean = true  // True = remote pointer (Z-yaw), False = vertical (Y-yaw)
) {
    private var emaX = 0f
    private var emaY = 0f

    private var accumDx = 0f
    private var accumDy = 0f

    private var biasX = 0f
    private var biasY = 0f

    /**
     * Resets filter and fractional accumulators.
     */
    fun reset() {
        emaX = 0f
        emaY = 0f
        accumDx = 0f
        accumDy = 0f
    }

    /**
     * Calibrate zero-drift offset from current resting position.
     */
    fun calibrate(currentGyroX: Float, currentGyroY: Float, currentGyroZ: Float) {
        val (rawX, rawY) = if (usePointerMode) {
            Pair(-currentGyroZ, -currentGyroX)
        } else {
            Pair(currentGyroY, -currentGyroX)
        }
        biasX = rawX
        biasY = rawY
        reset()
    }

    /**
     * Processes 3-axis gyroscope sensor event values (rad/s).
     * @return Pair<Int, Int> representing (dx, dy) clamped to -127..127.
     */
    fun process(gyroX: Float, gyroY: Float, gyroZ: Float): Pair<Int, Int> {
        // Map axes according to phone grip
        // Y-axis = rotation around vertical (or Z-axis in remote pointer mode)
        // X-axis = tilt forward/back (inverted sign for standard mouse screen space)
        val (rawX, rawY) = if (usePointerMode) {
            Pair(-gyroZ, -gyroX)
        } else {
            Pair(gyroY, -gyroX)
        }

        val directedX = (if (invertX) -rawX else rawX) - biasX
        val directedY = (if (invertY) -rawY else rawY) - biasY

        // 1. Deadzone filter (ignore micro-tremors below threshold)
        val deadzonedX = applyDeadzone(directedX, deadzone)
        val deadzonedY = applyDeadzone(directedY, deadzone)

        // 2. Exponential Moving Average (EMA) low-pass filter
        val a = alpha.coerceIn(0.05f, 0.95f)
        emaX = a * deadzonedX + (1f - a) * emaX
        emaY = a * deadzonedY + (1f - a) * emaY

        // 3. Sensitivity multiplier
        val targetDx = emaX * sensitivity
        val targetDy = emaY * sensitivity

        // 4. Sub-pixel accumulator to prevent discarding low-velocity movements
        accumDx += targetDx
        accumDy += targetDy

        val intDx = accumDx.toInt()
        val intDy = accumDy.toInt()

        accumDx -= intDx
        accumDy -= intDy

        // 5. Clamp to signed 8-bit HID bounds (-127..127)
        val clampedDx = intDx.coerceIn(-127, 127)
        val clampedDy = intDy.coerceIn(-127, 127)

        return Pair(clampedDx, clampedDy)
    }

    private fun applyDeadzone(value: Float, threshold: Float): Float {
        val mag = abs(value)
        if (mag <= threshold) return 0f
        return if (value > 0f) value - threshold else value + threshold
    }
}
