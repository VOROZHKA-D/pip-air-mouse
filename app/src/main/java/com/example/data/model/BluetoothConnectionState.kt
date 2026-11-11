package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * State representing Bluetooth HID Device status and connectivity.
 */
@Immutable
sealed interface BluetoothConnectionState {
    object NotInitialized : BluetoothConnectionState
    object BluetoothDisabled : BluetoothConnectionState
    object PermissionRequired : BluetoothConnectionState
    object HidNotSupported : BluetoothConnectionState
    object Ready : BluetoothConnectionState // Profile proxy ready, app registered, waiting for host
    object Connecting : BluetoothConnectionState
    data class Connected(val hostName: String, val address: String) : BluetoothConnectionState
    object Disconnected : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}
