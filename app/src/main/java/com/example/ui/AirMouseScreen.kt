package com.example.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AirMouseToggleSwitch
import com.example.ui.components.BluetoothGuideModal
import com.example.ui.components.LogViewModal
import com.example.ui.components.MouseButtonsSection
import com.example.ui.components.SettingsCard
import com.example.ui.components.StatusHeaderCard
import com.example.ui.components.VirtualCursorPad
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirMouseScreen(
    viewModel: AirMouseViewModel,
    onRequestBluetoothPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val isAirMouseActive by viewModel.isAirMouseActive.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val virtualCursor by viewModel.virtualCursor.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var showLogsModal by remember { mutableStateOf(false) }
    var showGuideModal by remember { mutableStateOf(false) }

    // Launcher for discoverable mode request (makes device discoverable to PC for 300 seconds)
    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPairedDevices()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = SlateBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Pip Air Mouse",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Bluetooth HID гиро-мышь",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.initBluetooth() },
                        modifier = Modifier.testTag("btn_reconnect_hid")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Перезапустить HID",
                            tint = CyanPrimary
                        )
                    }
                    IconButton(
                        onClick = { showGuideModal = true },
                        modifier = Modifier.testTag("btn_help_guide")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Справка по подключению",
                            tint = TextMuted
                        )
                    }
                    IconButton(
                        onClick = { showLogsModal = true },
                        modifier = Modifier.testTag("btn_open_logs")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Журнал ошибок",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = SlateBackground,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                MouseButtonsSection(
                    onLeftClick = { viewModel.performLeftClick() },
                    onRightClick = { viewModel.performRightClick() },
                    onButtonDown = { viewModel.onButtonDown(it) },
                    onButtonUp = { viewModel.onButtonUp(it) },
                    onCalibrateCenter = { viewModel.calibrateCenter() }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Connection state header card
            item {
                StatusHeaderCard(
                    state = connectionState,
                    pairedDevices = pairedDevices,
                    onConnectDevice = { viewModel.connectToDevice(it) },
                    onRequestPermissions = onRequestBluetoothPermissions,
                    onOpenBluetoothSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    onMakeDiscoverable = {
                        try {
                            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                            }
                            discoverableLauncher.launch(discoverableIntent)
                        } catch (e: SecurityException) {
                            onRequestBluetoothPermissions()
                        }
                    }
                )
            }

            // 2. Main Air Mouse ON/OFF Toggle Switch
            item {
                AirMouseToggleSwitch(
                    isActive = isAirMouseActive,
                    telemetry = telemetry,
                    onToggle = { viewModel.toggleAirMouse(it) }
                )
            }

            // 3. Virtual Cursor Canvas (interactive visual test area)
            item {
                VirtualCursorPad(
                    cursorNormalized = virtualCursor,
                    isAirMouseActive = isAirMouseActive,
                    onRecenter = { viewModel.calibrateCenter() }
                )
            }

            // 4. Sensitivity Slider and Settings Card
            item {
                SettingsCard(
                    settings = settings,
                    onSensitivityChange = { viewModel.updateSensitivity(it) },
                    onDeadzoneChange = { viewModel.updateDeadzone(it) },
                    onSmoothingChange = { viewModel.updateSmoothing(it) },
                    onInvertXChange = { viewModel.toggleInvertX(it) },
                    onInvertYChange = { viewModel.toggleInvertY(it) },
                    onGripModeChange = { viewModel.setGripMode(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showLogsModal) {
        LogViewModal(
            logs = logs,
            onClearLogs = { viewModel.clearLogs() },
            onDismiss = { showLogsModal = false }
        )
    }

    if (showGuideModal) {
        BluetoothGuideModal(
            onMakeDiscoverable = {
                try {
                    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    discoverableLauncher.launch(discoverableIntent)
                } catch (e: Exception) {
                    onRequestBluetoothPermissions()
                }
                showGuideModal = false
            },
            onOpenBluetoothSettings = {
                try {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                } catch (e: Exception) {
                    // Ignore
                }
                showGuideModal = false
            },
            onDismiss = { showGuideModal = false }
        )
    }
}
