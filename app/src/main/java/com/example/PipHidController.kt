package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.AirMouseLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class MouseReport(
    val buttons: Byte,
    val dx: Byte,
    val dy: Byte,
    val wheel: Byte
)

sealed interface PipHidState {
    object Idle : PipHidState                               // "Не запущено"
    object Registering : PipHidState                        // "Регистрация…"
    object Ready : PipHidState                              // "Открой Bluetooth на Mac → Pip Air Mouse"
    data class Connected(val hostName: String, val address: String) : PipHidState // "Подключено: <Mac name>"
    data class Error(val message: String) : PipHidState     // "Ошибка: <message>"
}

class PipHidController(private val context: Context) {

    companion object {
        const val TAG = "PipHid"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _state = MutableStateFlow<PipHidState>(PipHidState.Idle)
    val state: StateFlow<PipHidState> = _state.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    var connectedDevice: BluetoothDevice? = null
        private set

    private var isAppRegistered = false
    private val hidExecutor = Executors.newSingleThreadExecutor()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reportChannel = Channel<MouseReport>(Channel.CONFLATED)
    private var dispatcherJob: Job? = null

    private var currentButtons: Byte = 0x00

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered, pluggedDevice=$pluggedDevice")
            AirMouseLogger.i("[$TAG] onAppStatusChanged: registered=$registered")
            isAppRegistered = registered

            if (registered) {
                if (pluggedDevice != null) {
                    onDeviceConnected(pluggedDevice)
                } else {
                    _state.value = PipHidState.Ready
                }
            } else {
                _state.value = PipHidState.Error("Регистрация HID отклонена")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = try {
                device?.name ?: device?.address ?: "Хост"
            } catch (e: SecurityException) {
                device?.address ?: "Хост"
            }
            Log.d(TAG, "onConnectionStateChanged: device=$deviceName, state=$state")
            AirMouseLogger.i("[$TAG] onConnectionStateChanged: $deviceName state=$state")

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    device?.let { onDeviceConnected(it) }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = PipHidState.Registering
                }
                BluetoothProfile.STATE_DISCONNECTING,
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Device disconnected: $deviceName")
                    connectedDevice = null
                    _state.value = if (isAppRegistered) PipHidState.Ready else PipHidState.Idle
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
            AirMouseLogger.d("[$TAG] onGetReport: type=$type, id=$id")
            try {
                // Reply with current empty 4-byte report to fulfill host interrogation
                hidDevice?.replyReport(device, type, id, byteArrayOf(currentButtons, 0, 0, 0))
            } catch (e: Exception) {
                Log.w(TAG, "Error in replyReport: ${e.message}")
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "onSetReport: type=$type, id=$id, dataSize=${data?.size}")
            AirMouseLogger.d("[$TAG] onSetReport: id=$id")
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (e: Exception) {
                // Ignore
            }
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            Log.d(TAG, "onSetProtocol: protocol=$protocol")
            AirMouseLogger.i("[$TAG] onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Log.d(TAG, "onInterruptData: reportId=$reportId, size=${data?.size}")
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i(TAG, "BluetoothProfile.HID_DEVICE connected")
                AirMouseLogger.i("[$TAG] HID служба подключена")
                hidDevice = proxy as? BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.w(TAG, "BluetoothProfile.HID_DEVICE disconnected")
                AirMouseLogger.w("[$TAG] HID служба отключена")
                hidDevice = null
                isAppRegistered = false
                _state.value = PipHidState.Idle
            }
        }
    }

    init {
        startReportDispatcher()
    }

    private fun startReportDispatcher() {
        dispatcherJob?.cancel()
        dispatcherJob = scope.launch {
            for (report in reportChannel) {
                val target = connectedDevice
                val hid = hidDevice
                if (target != null && hid != null) {
                    val bytes = byteArrayOf(report.buttons, report.dx, report.dy, report.wheel)
                    try {
                        hid.sendReport(target, 0, bytes)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException while sending report", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending report: ${e.message}")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onDeviceConnected(device: BluetoothDevice) {
        connectedDevice = device
        val name = try {
            device.name ?: device.address ?: "Mac"
        } catch (e: SecurityException) {
            device.address ?: "Mac"
        }
        _state.value = PipHidState.Connected(name, device.address)

        // macOS CRITICAL FIX: Send a zero report [0, 0, 0, 0] immediately upon connection
        // to wake up macOS HID event queue and negotiate report protocol
        scope.launch {
            delay(100)
            mouse(0, 0, 0, 0)
            Log.i(TAG, "Sent initial wakeup mouse report [0, 0, 0, 0] to $name")
        }
    }

    fun start() {
        if (bluetoothAdapter == null) {
            _state.value = PipHidState.Error("Это устройство не поддерживает Bluetooth")
            return
        }

        if (!hasPermissions()) {
            _state.value = PipHidState.Error("Нет разрешений Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _state.value = PipHidState.Error("Включите Bluetooth")
            return
        }

        _state.value = PipHidState.Registering
        Log.i(TAG, "Acquiring HID_DEVICE profile proxy...")

        try {
            val requested = bluetoothAdapter.getProfileProxy(
                context,
                profileListener,
                BluetoothProfile.HID_DEVICE
            )
            if (!requested) {
                _state.value = PipHidState.Error("HID-профиль недоступен на этом телефоне")
            }
        } catch (e: SecurityException) {
            _state.value = PipHidState.Error("Нет разрешений Bluetooth")
        } catch (e: Exception) {
            _state.value = PipHidState.Error("Ошибка HID: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val hid = hidDevice
        if (hid == null) {
            _state.value = PipHidState.Error("HID-профиль недоступен на этом телефоне")
            return
        }

        if (!hasPermissions()) {
            _state.value = PipHidState.Error("Нет разрешений Bluetooth")
            return
        }

        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "Pip Air Mouse",
                "Android BLE mouse",
                "Pip",
                BluetoothHidDevice.SUBCLASS1_MOUSE, // Required for macOS recognition
                HidDescriptor.MOUSE_REPORT_DESCRIPTOR
            )

            Log.i(TAG, "Registering App with SUBCLASS1_MOUSE (Report ID = 0, 4-byte report)...")
            val ok = hid.registerApp(
                sdp,
                null,
                null,
                hidExecutor,
                hidCallback
            )

            if (!ok) {
                _state.value = PipHidState.Error("Не удалось зарегистрировать HID. Попробуйте перезапустить Bluetooth")
            }
        } catch (e: SecurityException) {
            _state.value = PipHidState.Error("Нет разрешений Bluetooth")
        } catch (e: Exception) {
            _state.value = PipHidState.Error("Ошибка регистрации: ${e.message}")
        }
    }

    /**
     * Sends relative mouse movement (buttons, dx, dy, wheel).
     * Dispatched through batched Channel to avoid any UI thread jank.
     */
    fun mouse(buttons: Byte, dx: Int, dy: Int, wheel: Int = 0) {
        currentButtons = buttons
        val packet = MouseReport(
            buttons = buttons,
            dx = dx.coerceIn(-127, 127).toByte(),
            dy = dy.coerceIn(-127, 127).toByte(),
            wheel = wheel.coerceIn(-127, 127).toByte()
        )
        reportChannel.trySend(packet)
    }

    /**
     * Left Click: Press (0x01) and immediately release (0x00).
     */
    fun clickLeft() {
        scope.launch {
            mouse(HidDescriptor.BUTTON_LEFT, 0, 0, 0)
            delay(60)
            mouse(HidDescriptor.BUTTON_NONE, 0, 0, 0)
        }
    }

    /**
     * Double Click Left: Two rapid clicks with 100ms pause.
     */
    fun doubleClickLeft() {
        scope.launch {
            clickLeft()
            delay(100)
            clickLeft()
        }
    }

    /**
     * Right Click: Press (0x02) and immediately release (0x00).
     */
    fun clickRight() {
        scope.launch {
            mouse(HidDescriptor.BUTTON_RIGHT, 0, 0, 0)
            delay(60)
            mouse(HidDescriptor.BUTTON_NONE, 0, 0, 0)
        }
    }

    /**
     * Middle Click: Press (0x04) and immediately release (0x00).
     */
    fun clickMiddle() {
        scope.launch {
            mouse(HidDescriptor.BUTTON_MIDDLE, 0, 0, 0)
            delay(60)
            mouse(HidDescriptor.BUTTON_NONE, 0, 0, 0)
        }
    }

    /**
     * Scroll wheel delta (-127..127).
     */
    fun scroll(wheelDelta: Int) {
        mouse(currentButtons, 0, 0, wheelDelta)
    }

    fun setButtonDown(button: Byte) {
        currentButtons = (currentButtons.toInt() or button.toInt()).toByte()
        mouse(currentButtons, 0, 0, 0)
    }

    fun setButtonUp(button: Byte) {
        currentButtons = (currentButtons.toInt() and button.toInt().inv()).toByte()
        mouse(currentButtons, 0, 0, 0)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            if (isAppRegistered && hidDevice != null && hasPermissions()) {
                hidDevice?.unregisterApp()
            }
            if (bluetoothAdapter != null && hidDevice != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping PipHidController: ${e.message}")
        } finally {
            hidDevice = null
            connectedDevice = null
            isAppRegistered = false
            _state.value = PipHidState.Idle
        }
    }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val advertise = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
            return connect && advertise
        }
        return true
    }
}
