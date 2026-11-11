package com.example.bluetooth

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
import androidx.core.content.ContextCompat
import com.example.data.model.BluetoothConnectionState
import com.example.util.AirMouseLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * Manages the Bluetooth HID Device profile, app registration,
 * and transmission of HID mouse reports to a connected host (PC/Mac).
 */
class BluetoothHidManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState =
        MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.NotInitialized)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    var connectedDevice: BluetoothDevice? = null
        private set

    private var isAppRegistered = false
    private val hidExecutor = Executors.newSingleThreadExecutor()

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isAppRegistered = registered
            if (registered) {
                AirMouseLogger.i("HID профиль успешно зарегистрирован в системе")
                if (pluggedDevice != null) {
                    handleDeviceConnected(pluggedDevice)
                } else {
                    _connectionState.value = BluetoothConnectionState.Ready
                }
            } else {
                AirMouseLogger.w("HID профиль разрегистрирован")
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = try {
                device?.name ?: device?.address ?: "Хост"
            } catch (e: SecurityException) {
                device?.address ?: "Хост"
            }

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    AirMouseLogger.i("Подключено к хосту: $deviceName (${device?.address})")
                    device?.let { handleDeviceConnected(it) }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    AirMouseLogger.i("Установка соединения с: $deviceName...")
                    _connectionState.value = BluetoothConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    AirMouseLogger.i("Отключение от: $deviceName...")
                    _connectionState.value = BluetoothConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    AirMouseLogger.i("Хост $deviceName отключен")
                    connectedDevice = null
                    _connectionState.value = if (isAppRegistered) {
                        BluetoothConnectionState.Ready
                    } else {
                        BluetoothConnectionState.Disconnected
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            AirMouseLogger.d("onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
            // Default reply with empty report to satisfy host interrogation
            try {
                hidDevice?.replyReport(device, type, id, byteArrayOf(0, 0, 0))
            } catch (e: Exception) {
                AirMouseLogger.w("Не удалось ответить на onGetReport: ${e.message}")
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            AirMouseLogger.d("onSetReport: type=$type, id=$id")
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (e: Exception) {
                // Ignore
            }
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            AirMouseLogger.d("onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            AirMouseLogger.d("onInterruptData: reportId=$reportId")
        }
    }

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                AirMouseLogger.i("Служба Bluetooth HID Device подключена")
                hidDevice = proxy as? BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                AirMouseLogger.w("Служба Bluetooth HID Device отключена")
                hidDevice = null
                isAppRegistered = false
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }
    }

    /**
     * Initializes Bluetooth HID Device profile proxy.
     */
    fun initialize() {
        if (bluetoothAdapter == null) {
            AirMouseLogger.e("Bluetooth адаптер отсутствует на этом устройстве")
            _connectionState.value = BluetoothConnectionState.HidNotSupported
            return
        }

        if (!hasRequiredPermissions()) {
            AirMouseLogger.w("Ожидание разрешений Bluetooth от пользователя")
            _connectionState.value = BluetoothConnectionState.PermissionRequired
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            AirMouseLogger.w("Bluetooth отключен в настройках системы")
            _connectionState.value = BluetoothConnectionState.BluetoothDisabled
            return
        }

        try {
            AirMouseLogger.i("Запрос прокси-профиля BluetoothProfile.HID_DEVICE...")
            val success = bluetoothAdapter.getProfileProxy(
                context,
                profileServiceListener,
                BluetoothProfile.HID_DEVICE
            )
            if (!success) {
                AirMouseLogger.e("getProfileProxy вернул false. Устройство может не поддерживать HID Device.")
                _connectionState.value = BluetoothConnectionState.HidNotSupported
            }
        } catch (e: SecurityException) {
            AirMouseLogger.e("SecurityException при запросе HID прокси", e)
            _connectionState.value = BluetoothConnectionState.PermissionRequired
        } catch (e: Exception) {
            AirMouseLogger.e("Ошибка при инициализации HID профиля", e)
            _connectionState.value = BluetoothConnectionState.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val device = hidDevice ?: return
        if (!hasRequiredPermissions()) {
            _connectionState.value = BluetoothConnectionState.PermissionRequired
            return
        }

        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                HidConstants.SDP_NAME,
                HidConstants.SDP_DESCRIPTION,
                HidConstants.SDP_PROVIDER,
                HidConstants.SUBCLASS_MOUSE,
                HidConstants.MOUSE_REPORT_DESCRIPTOR
            )

            AirMouseLogger.i("Регистрация SDP настроек мыши (SUBCLASS1_MOUSE, Report ID = 0)...")
            val registered = device.registerApp(
                sdp,
                null,
                null,
                hidExecutor,
                hidCallback
            )

            if (!registered) {
                AirMouseLogger.w("device.registerApp() вернул false. Ожидаем callback...")
            }
        } catch (e: SecurityException) {
            AirMouseLogger.e("SecurityException при регистрации HID приложения", e)
            _connectionState.value = BluetoothConnectionState.PermissionRequired
        } catch (e: Exception) {
            AirMouseLogger.e("Ошибка регистрации приложения HID", e)
            _connectionState.value = BluetoothConnectionState.Error(e.message ?: "Ошибка регистрации HID")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceConnected(device: BluetoothDevice) {
        connectedDevice = device
        val name = try {
            device.name ?: device.address ?: "Хост"
        } catch (e: SecurityException) {
            device.address ?: "Хост"
        }
        _connectionState.value = BluetoothConnectionState.Connected(name, device.address)
    }

    /**
     * Sends standard 3-byte mouse report (buttons, dx, dy) via Bluetooth HID Device.
     * @param buttons bitmask of pressed buttons (0x01 Left, 0x02 Right, 0x04 Middle)
     * @param dx relative X displacement clamped to -127..127
     * @param dy relative Y displacement clamped to -127..127
     */
    @SuppressLint("MissingPermission")
    fun sendReport(buttons: Byte, dx: Int, dy: Int): Boolean {
        val target = connectedDevice ?: return false
        val hid = hidDevice ?: return false

        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()
        val reportData = byteArrayOf(buttons, clampedDx, clampedDy)

        return try {
            hid.sendReport(target, HidConstants.REPORT_ID_MOUSE, reportData)
        } catch (e: SecurityException) {
            AirMouseLogger.e("SecurityException при отправке отчета HID", e)
            false
        } catch (e: Exception) {
            AirMouseLogger.e("Ошибка при sendReport: ${e.message}", e)
            false
        }
    }

    /**
     * Connects to a previously paired Bluetooth device directly.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice): Boolean {
        val hid = hidDevice ?: return false
        return try {
            AirMouseLogger.i("Попытка прямого подключения к ${device.name ?: device.address}...")
            hid.connect(device)
        } catch (e: Exception) {
            AirMouseLogger.e("Ошибка подключения к устройству: ${e.message}", e)
            false
        }
    }

    /**
     * Returns list of bonded (paired) devices that can be targeted.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasRequiredPermissions() || bluetoothAdapter == null) return emptyList()
        return try {
            bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            AirMouseLogger.e("SecurityException при чтении сопряженных устройств", e)
            emptyList()
        }
    }

    /**
     * Unregisters HID app and releases profile proxy.
     */
    @SuppressLint("MissingPermission")
    fun release() {
        try {
            if (isAppRegistered && hidDevice != null && hasRequiredPermissions()) {
                hidDevice?.unregisterApp()
            }
            if (bluetoothAdapter != null && hidDevice != null) {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (e: Exception) {
            AirMouseLogger.w("Ошибка при освобождении HID ресурсов: ${e.message}")
        } finally {
            hidDevice = null
            connectedDevice = null
            isAppRegistered = false
        }
    }

    fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val advertiseGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
            return connectGranted && advertiseGranted
        }
        return true
    }
}
