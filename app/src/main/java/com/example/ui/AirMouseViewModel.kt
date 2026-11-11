package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothHidManager
import com.example.bluetooth.HidConstants
import com.example.data.model.AirMouseSettings
import com.example.data.model.BluetoothConnectionState
import com.example.data.model.GripMode
import com.example.data.model.LogEntry
import com.example.data.model.SensorTelemetry
import com.example.sensor.AirMouseSensorManager
import com.example.util.AirMouseLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AirMouseViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    val bluetoothHidManager = BluetoothHidManager(context)

    private val _isAirMouseActive = MutableStateFlow(false)
    val isAirMouseActive: StateFlow<Boolean> = _isAirMouseActive.asStateFlow()

    private val _settings = MutableStateFlow(AirMouseSettings())
    val settings: StateFlow<AirMouseSettings> = _settings.asStateFlow()

    private val _virtualCursor = MutableStateFlow(Offset(0.5f, 0.5f)) // Normalized 0..1
    val virtualCursor: StateFlow<Offset> = _virtualCursor.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private var activeButtonsMask: Byte = 0x00

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothHidManager.connectionState
    val logs: StateFlow<List<LogEntry>> = AirMouseLogger.logs

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sensorManager = AirMouseSensorManager(context) { dx, dy ->
        handleMovement(dx, dy)
    }

    val telemetry: StateFlow<SensorTelemetry> = sensorManager.telemetry
    val isGyroscopeAvailable: Boolean = sensorManager.isGyroscopeAvailable

    init {
        AirMouseLogger.i("Запуск Pip Air Mouse...")
        sensorManager.currentSettings = _settings.value
        refreshPairedDevices()
    }

    fun initBluetooth() {
        bluetoothHidManager.initialize()
        refreshPairedDevices()
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothHidManager.getPairedDevices()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothHidManager.connectToDevice(device)
    }

    fun toggleAirMouse(enabled: Boolean) {
        _isAirMouseActive.value = enabled
        if (enabled) {
            triggerHaptic(60)
            sensorManager.startListening()
            AirMouseLogger.i("Air Mouse включен пользователем")
        } else {
            triggerHaptic(30)
            sensorManager.stopListening()
            // Clear any lingering button state
            if (activeButtonsMask != 0.toByte()) {
                activeButtonsMask = 0
                bluetoothHidManager.sendReport(0, 0, 0)
            }
            AirMouseLogger.i("Air Mouse выключен пользователем")
        }
    }

    private fun handleMovement(dx: Int, dy: Int) {
        if (!_isAirMouseActive.value) return

        // 1. Send HID report to connected PC/Mac
        val sent = bluetoothHidManager.sendReport(activeButtonsMask, dx, dy)
        if (sent) {
            // Increment reports sent count
        }

        // 2. Update virtual cursor preview for on-screen test mode
        _virtualCursor.update { current ->
            val stepScale = 0.003f
            val newX = (current.x + dx * stepScale).coerceIn(0.05f, 0.95f)
            val newY = (current.y + dy * stepScale).coerceIn(0.05f, 0.95f)
            Offset(newX, newY)
        }
    }

    /**
     * Sends Left Click (bitmask 0x01) and then resets to 0.
     */
    fun performLeftClick() {
        viewModelScope.launch {
            triggerHaptic(40)
            AirMouseLogger.d("Клик: Левая кнопка (0x01)")
            activeButtonsMask = (activeButtonsMask.toInt() or HidConstants.BUTTON_LEFT.toInt()).toByte()
            bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
            delay(50)
            activeButtonsMask = (activeButtonsMask.toInt() and HidConstants.BUTTON_LEFT.toInt().inv()).toByte()
            bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
        }
    }

    /**
     * Sends Right Click (bitmask 0x02) and then resets to 0.
     */
    fun performRightClick() {
        viewModelScope.launch {
            triggerHaptic(50)
            AirMouseLogger.d("Клик: Правая кнопка (0x02)")
            activeButtonsMask = (activeButtonsMask.toInt() or HidConstants.BUTTON_RIGHT.toInt()).toByte()
            bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
            delay(50)
            activeButtonsMask = (activeButtonsMask.toInt() and HidConstants.BUTTON_RIGHT.toInt().inv()).toByte()
            bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
        }
    }

    /**
     * Handles touch-down and touch-up events for click & drag support.
     */
    fun onButtonDown(buttonBit: Byte) {
        triggerHaptic(30)
        activeButtonsMask = (activeButtonsMask.toInt() or buttonBit.toInt()).toByte()
        bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
    }

    fun onButtonUp(buttonBit: Byte) {
        activeButtonsMask = (activeButtonsMask.toInt() and buttonBit.toInt().inv()).toByte()
        bluetoothHidManager.sendReport(activeButtonsMask, 0, 0)
    }

    fun updateSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 2.5f)
        _settings.update { it.copy(sensitivity = clamped) }
        sensorManager.currentSettings = _settings.value
    }

    fun updateDeadzone(value: Float) {
        val clamped = value.coerceIn(0.01f, 0.15f)
        _settings.update { it.copy(deadzone = clamped) }
        sensorManager.currentSettings = _settings.value
    }

    fun updateSmoothing(value: Float) {
        val clamped = value.coerceIn(0.05f, 0.90f)
        _settings.update { it.copy(smoothingAlpha = clamped) }
        sensorManager.currentSettings = _settings.value
    }

    fun toggleInvertX(invert: Boolean) {
        _settings.update { it.copy(invertX = invert) }
        sensorManager.currentSettings = _settings.value
    }

    fun toggleInvertY(invert: Boolean) {
        _settings.update { it.copy(invertY = invert) }
        sensorManager.currentSettings = _settings.value
    }

    fun setGripMode(mode: GripMode) {
        _settings.update { it.copy(gripMode = mode) }
        sensorManager.currentSettings = _settings.value
    }

    fun toggleTestMode(enabled: Boolean) {
        _settings.update { it.copy(testMode = enabled) }
    }

    fun calibrateCenter() {
        triggerHaptic(80)
        sensorManager.calibrateCenter()
        _virtualCursor.value = Offset(0.5f, 0.5f)
    }

    fun clearLogs() {
        AirMouseLogger.clear()
    }

    fun onResume() {
        if (_isAirMouseActive.value) {
            sensorManager.startListening()
        }
        refreshPairedDevices()
    }

    fun onPause() {
        // Save battery when activity is paused
        sensorManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        bluetoothHidManager.release()
    }

    private fun triggerHaptic(durationMs: Long) {
        if (!_settings.value.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails or permission denied
        }
    }
}
