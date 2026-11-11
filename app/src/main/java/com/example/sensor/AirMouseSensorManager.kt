package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.model.AirMouseSettings
import com.example.data.model.GripMode
import com.example.data.model.SensorTelemetry
import com.example.util.AirMouseLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

/**
 * Reads phone gyroscope rotation in real-time, applies deadzone,
 * low-pass EMA filter, sensitivity scaling, and produces relative mouse movement.
 */
class AirMouseSensorManager(
    context: Context,
    private val onMovementCalculated: (dx: Int, dy: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    private var isListening = false

    // EMA state
    private var emaX = 0f
    private var emaY = 0f

    // Sub-pixel fractional accumulator
    private var accumulatedDx = 0f
    private var accumulatedDy = 0f

    // Calibration offsets
    private var biasX = 0f
    private var biasY = 0f

    var currentSettings = AirMouseSettings()
        set(value) {
            field = value
        }

    val isGyroscopeAvailable: Boolean
        get() = gyroscope != null

    fun startListening() {
        if (isListening) return
        if (gyroscope == null) {
            AirMouseLogger.w("Гироскоп отсутствует на устройстве")
            return
        }

        // SENSOR_DELAY_GAME provides ~50Hz sampling (optimal for HID mouse latency and battery)
        val registered = sensorManager?.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME
        ) ?: false

        if (registered) {
            isListening = true
            emaX = 0f
            emaY = 0f
            accumulatedDx = 0f
            accumulatedDy = 0f
            AirMouseLogger.i("Гироскоп запущен (SENSOR_DELAY_GAME)")
        } else {
            AirMouseLogger.e("Не удалось зарегистрировать слушатель гироскопа")
        }
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        emaX = 0f
        emaY = 0f
        accumulatedDx = 0f
        accumulatedDy = 0f
        _telemetry.update {
            it.copy(
                dx = 0,
                dy = 0,
                isMoving = false
            )
        }
        AirMouseLogger.i("Гироскоп остановлен (экономия батареи)")
    }

    fun calibrateCenter() {
        // Record current gyro readings as bias to eliminate stationary drift
        val current = _telemetry.value
        biasX = current.rawGyroX
        biasY = if (currentSettings.gripMode == GripMode.POINTER) current.rawGyroZ else current.rawGyroY
        emaX = 0f
        emaY = 0f
        accumulatedDx = 0f
        accumulatedDy = 0f
        AirMouseLogger.i("Центровка/калибровка дрейфа выполнена: biasX=$biasX, biasY=$biasY")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // Map physical rotation axes based on grip mode
        val (axisMotionX, axisMotionY) = when (currentSettings.gripMode) {
            GripMode.POINTER -> {
                // TV remote style: turning wrist yaw (Z axis) moves cursor horizontally
                // Tilting wrist pitch (X axis) moves cursor vertically
                val x = -rawZ
                val y = -rawX
                Pair(x, y)
            }
            GripMode.PORTRAIT -> {
                // Vertical phone: roll (Y axis) or yaw moves horizontally
                val x = -rawY
                val y = -rawX
                Pair(x, y)
            }
        }

        // Apply axis inversion
        val directedX = if (currentSettings.invertX) -axisMotionX else axisMotionX
        val directedY = if (currentSettings.invertY) -axisMotionY else axisMotionY

        // Subtract bias
        val biasedX = directedX - biasX
        val biasedY = directedY - biasY

        // 1. Deadzone filter (e.g. 0.04 rad/s)
        val deadzonedX = applyDeadzone(biasedX, currentSettings.deadzone)
        val deadzonedY = applyDeadzone(biasedY, currentSettings.deadzone)

        // 2. Low-pass filter (Exponential Moving Average)
        val alpha = currentSettings.smoothingAlpha.coerceIn(0.05f, 0.95f)
        emaX = alpha * deadzonedX + (1f - alpha) * emaX
        emaY = alpha * deadzonedY + (1f - alpha) * emaY

        // 3. Sensitivity scaling and rad/s to pixel translation
        val baseScale = 28.0f
        val scaledDx = emaX * baseScale * currentSettings.sensitivity
        val scaledDy = emaY * baseScale * currentSettings.sensitivity

        // 4. Fractional sub-pixel accumulator
        accumulatedDx += scaledDx
        accumulatedDy += scaledDy

        val intDx = accumulatedDx.toInt()
        val intDy = accumulatedDy.toInt()

        accumulatedDx -= intDx
        accumulatedDy -= intDy

        // Clamp to -127..127 per HID mouse specification
        val clampedDx = intDx.coerceIn(-127, 127)
        val clampedDy = intDy.coerceIn(-127, 127)

        val isMoving = clampedDx != 0 || clampedDy != 0

        _telemetry.update { current ->
            current.copy(
                rawGyroX = rawX,
                rawGyroY = rawY,
                rawGyroZ = rawZ,
                dx = clampedDx,
                dy = clampedDy,
                isMoving = isMoving,
                eventsCount = current.eventsCount + 1L
            )
        }

        if (isMoving) {
            onMovementCalculated(clampedDx, clampedDy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun applyDeadzone(value: Float, deadzone: Float): Float {
        val magnitude = abs(value)
        if (magnitude <= deadzone) {
            return 0f
        }
        return if (value > 0f) value - deadzone else value + deadzone
    }
}
