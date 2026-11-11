package com.example.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BluetoothConnectionState
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusHeaderCard(
    state: BluetoothConnectionState,
    pairedDevices: List<BluetoothDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeviceMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (badgeColor, statusTitle, statusSubtitle, icon) = when (state) {
        is BluetoothConnectionState.Connected -> {
            Quad(
                NeonGreen,
                "Подключено к ${state.hostName}",
                state.address,
                Icons.Default.BluetoothConnected
            )
        }
        is BluetoothConnectionState.Ready -> {
            Quad(
                CyanPrimary,
                "Готово (Ожидание хоста)",
                "Выберите 'Pip Air Mouse' на ПК или Mac",
                Icons.Default.Bluetooth
            )
        }
        is BluetoothConnectionState.Connecting -> {
            Quad(
                NeonAmber,
                "Подключение к хосту...",
                "Инициализация HID канала",
                Icons.Default.Bluetooth
            )
        }
        is BluetoothConnectionState.BluetoothDisabled -> {
            Quad(
                NeonAmber,
                "Bluetooth выключен",
                "Включите Bluetooth для подключения",
                Icons.Default.BluetoothDisabled
            )
        }
        is BluetoothConnectionState.PermissionRequired -> {
            Quad(
                NeonAmber,
                "Требуются разрешения",
                "Необходим доступ к Bluetooth (Android 12+)",
                Icons.Default.Warning
            )
        }
        is BluetoothConnectionState.HidNotSupported -> {
            Quad(
                NeonRed,
                "HID Device не поддерживается",
                "Устройство не поддерживает Bluetooth HID Device",
                Icons.Default.ErrorOutline
            )
        }
        is BluetoothConnectionState.Error -> {
            Quad(
                NeonRed,
                "Ошибка",
                state.message,
                Icons.Default.ErrorOutline
            )
        }
        is BluetoothConnectionState.Disconnected -> {
            Quad(
                Color.Gray,
                "Отключено",
                "Соединение с хостом разорвано",
                Icons.Default.Bluetooth
            )
        }
        BluetoothConnectionState.NotInitialized -> {
            Quad(
                Color.Gray,
                "Инициализация...",
                "Подготовка Bluetooth HID стека",
                Icons.Default.Bluetooth
            )
        }
    }

    val animatedColor by animateColorAsState(targetValue = badgeColor, label = "badgeColor")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, animatedColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .testTag("status_card"),
        color = CardSurface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(animatedColor)
                            .alpha(if (state is BluetoothConnectionState.Ready || state is BluetoothConnectionState.Connected) pulseAlpha else 1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "СТАТУС ПОДКЛЮЧЕНИЯ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Paired devices quick selector
                if (pairedDevices.isNotEmpty()) {
                    Box {
                        IconButton(
                            onClick = { showDeviceMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_paired_devices")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Сопряженные устройства",
                                tint = CyanPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showDeviceMenu,
                            onDismissRequest = { showDeviceMenu = false }
                        ) {
                            Text(
                                text = "Сопряженные ПК/хосты:",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            pairedDevices.forEach { device ->
                                val name = try {
                                    device.name ?: device.address
                                } catch (e: SecurityException) {
                                    device.address
                                }
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    },
                                    onClick = {
                                        showDeviceMenu = false
                                        onConnectDevice(device)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = animatedColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick action buttons depending on state
            when (state) {
                is BluetoothConnectionState.PermissionRequired -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_request_permissions"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                    ) {
                        Text("Предоставить разрешения Bluetooth", fontWeight = FontWeight.Bold)
                    }
                }
                is BluetoothConnectionState.BluetoothDisabled -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenBluetoothSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_enable_bluetooth"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black)
                    ) {
                        Text("Открыть настройки Bluetooth", fontWeight = FontWeight.Bold)
                    }
                }
                is BluetoothConnectionState.Ready -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onMakeDiscoverable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_make_discoverable"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сделать телефон видимым для ПК (300 сек)")
                    }
                }
                else -> { /* No extra action required */ }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
