package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TouchpadSettings
import com.example.ui.components.BluetoothGuideModal
import com.example.ui.components.LogViewModal
import com.example.ui.components.PrecisionSettingsCard
import com.example.ui.components.TouchpadSurface
import com.example.ui.theme.PipAirMouseTheme
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.SyneFontFamily
import com.example.util.AirMouseLogger
import kotlin.math.roundToInt

enum class AppMode(val title: String, val icon: ImageVector) {
    TOUCHPAD("Трекпад", Icons.Default.TouchApp),
    AIR_MOUSE("Air Mouse", Icons.Default.Adjust),
    SETTINGS("Настройки", Icons.Default.Tune)
}

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var pipHid: PipHidController
    private lateinit var airMouseEngine: AirMouseEngine
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    private var selectedMode by mutableStateOf(AppMode.TOUCHPAD)
    private var isAirMouseEnabled by mutableStateOf(false)
    private var sensitivityVal by mutableFloatStateOf(45.0f)
    private var touchpadSettings by mutableStateOf(TouchpadSettings())
    private var isSensorListening = false
    private var hasGyroscope by mutableStateOf(true)

    // Telemetry state
    private var lastDx by mutableStateOf(0)
    private var lastDy by mutableStateOf(0)
    private var isMoving by mutableStateOf(false)
    private var motionEventsCount by mutableLongStateOf(0L)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            AirMouseLogger.i("[PipHid] Разрешения предоставлены, запуск HID...")
            pipHid.start()
        } else {
            AirMouseLogger.w("[PipHid] Разрешения не предоставлены пользователем")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pipHid = PipHidController(this)
        airMouseEngine = AirMouseEngine(sensitivity = sensitivityVal)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        hasGyroscope = (gyroscope != null)

        if (!hasGyroscope) {
            AirMouseLogger.w("[PipHid] Гироскоп не найден на устройстве!")
        }

        setContent {
            PipAirMouseTheme {
                MainAirMouseContent()
            }
        }

        requestPermissionsAndStart()
    }

    override fun onStart() {
        super.onStart()
        if (pipHid.hasPermissions()) {
            pipHid.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAirMouseEnabled && selectedMode == AppMode.AIR_MOUSE) {
            startGyroscope()
        }
    }

    override fun onPause() {
        super.onPause()
        stopGyroscope()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGyroscope()
        pipHid.stop()
    }

    private fun requestPermissionsAndStart() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            pipHid.start()
        }
    }

    private fun toggleAirMouse(enable: Boolean) {
        isAirMouseEnabled = enable
        if (enable) {
            airMouseEngine.reset()
            startGyroscope()
            AirMouseLogger.i("[Sensor] Air Mouse ВКЛ (Гироскоп запущен)")
        } else {
            stopGyroscope()
            isMoving = false
            AirMouseLogger.i("[Sensor] Air Mouse ВЫКЛ")
        }
    }

    private fun startGyroscope() {
        if (!isSensorListening && gyroscope != null) {
            val registered = sensorManager?.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            ) ?: false
            isSensorListening = registered
            if (registered) {
                AirMouseLogger.d("[Sensor] Гироскоп зарегистрирован (SENSOR_DELAY_GAME)")
            } else {
                AirMouseLogger.w("[Sensor] Не удалось зарегистрировать слушатель гироскопа")
            }
        }
    }

    private fun stopGyroscope() {
        if (isSensorListening) {
            sensorManager?.unregisterListener(this)
            isSensorListening = false
            isMoving = false
            AirMouseLogger.d("[Sensor] Гироскоп остановлен")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isAirMouseEnabled || selectedMode != AppMode.AIR_MOUSE) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val (dx, dy) = airMouseEngine.process(
                gyroX = event.values[0],
                gyroY = event.values[1],
                gyroZ = event.values[2]
            )

            lastDx = dx
            lastDy = dy
            isMoving = (dx != 0 || dy != 0)

            if (isMoving) {
                motionEventsCount++
                pipHid.mouse(HidDescriptor.BUTTON_NONE, dx, dy, 0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    private fun MainAirMouseContent() {
        val state by pipHid.state.collectAsState()
        val logs by AirMouseLogger.logs.collectAsState()
        var showLogs by remember { mutableStateOf(false) }
        var showGuide by remember { mutableStateOf(false) }

        val bgDark = Color(0xFF090A0E)

        val discoverableLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {}

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(bgDark),
            containerColor = bgDark,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgDark)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Minimalist Top Bar: Micro Logo on Left, Action icons on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Micro Brand Logo (Low-profile, stylish, small)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF7A00), Color(0xFFFF3D00))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = Color(0xFF090A0E),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(9.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "pip",
                                    fontFamily = SyneFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFF2F2F6),
                                    letterSpacing = 1.1.sp
                                )
                                Text(
                                    text = " • air mouse",
                                    fontFamily = PlusJakartaFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF9E40),
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }

                        // Right icons: Help `?` and Logs Terminal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Question Mark Guide Icon Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14161E))
                                    .border(1.dp, Color(0x33FF6D00), CircleShape)
                                    .clickable { showGuide = true }
                                    .testTag("btn_help_guide"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Инструкция и помощь",
                                    tint = Color(0xFFFF9E40),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Logs Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14161E))
                                    .border(1.dp, Color(0xFF222530), CircleShape)
                                    .clickable { showLogs = true }
                                    .testTag("btn_logs"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Логи",
                                    tint = Color(0xFF888894),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Ergonomic Segmented Menu Bar
                    ErgonomicSegmentedBar(
                        selectedMode = selectedMode,
                        onModeSelected = { mode ->
                            selectedMode = mode
                            if (mode == AppMode.AIR_MOUSE && isAirMouseEnabled) {
                                startGyroscope()
                            } else {
                                stopGyroscope()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Compact Bluetooth Status Capsule (Minimalist, unobtrusive)
                    MiniBluetoothPill(
                        state = state,
                        onMakeDiscoverable = {
                            try {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                }
                                discoverableLauncher.launch(intent)
                            } catch (e: Exception) {
                                requestPermissionsAndStart()
                            }
                        },
                        onRetry = { pipHid.start() },
                        onRequestPermissions = { requestPermissionsAndStart() }
                    )
                }
            },
            bottomBar = {
                // Minimalist Dual-Pad Mouse Buttons: Clean, Beautiful, ZERO Text!
                MinimalDualMouseBar(
                    onLeftClick = { pipHid.clickLeft() },
                    onLeftDown = { pipHid.setButtonDown(HidDescriptor.BUTTON_LEFT) },
                    onLeftUp = { pipHid.setButtonUp(HidDescriptor.BUTTON_LEFT) },
                    onRightClick = { pipHid.clickRight() },
                    onRightDown = { pipHid.setButtonDown(HidDescriptor.BUTTON_RIGHT) },
                    onRightUp = { pipHid.setButtonUp(HidDescriptor.BUTTON_RIGHT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                when (selectedMode) {
                    AppMode.TOUCHPAD -> {
                        // Minimalist Trackpad Canvas
                        TouchpadSurface(
                            pipHid = pipHid,
                            settings = touchpadSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppMode.AIR_MOUSE -> {
                        // Futuristic Minimal Air Mouse Console
                        AirMouseMinimalConsole(
                            isEnabled = isAirMouseEnabled,
                            hasGyroscope = hasGyroscope,
                            isMoving = isMoving,
                            dx = lastDx,
                            dy = lastDy,
                            sensitivity = sensitivityVal,
                            onSensitivityChange = {
                                sensitivityVal = it
                                airMouseEngine.sensitivity = it
                            },
                            onToggle = { toggleAirMouse(it) },
                            onRecenter = { airMouseEngine.reset() }
                        )
                    }

                    AppMode.SETTINGS -> {
                        // Refined Precision Settings
                        PrecisionSettingsCard(
                            settings = touchpadSettings,
                            onSettingsChange = { touchpadSettings = it },
                            gyroSensitivity = sensitivityVal,
                            onGyroSensitivityChange = {
                                sensitivityVal = it
                                airMouseEngine.sensitivity = it
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (showLogs) {
            LogViewModal(
                logs = logs,
                onClearLogs = { AirMouseLogger.clear() },
                onDismiss = { showLogs = false }
            )
        }

        if (showGuide) {
            BluetoothGuideModal(
                onMakeDiscoverable = {
                    try {
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                        }
                        discoverableLauncher.launch(intent)
                    } catch (e: Exception) {
                        requestPermissionsAndStart()
                    }
                    showGuide = false
                },
                onOpenBluetoothSettings = {
                    try {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (e: Exception) {}
                    showGuide = false
                },
                onDismiss = { showGuide = false }
            )
        }
    }
}

/**
 * Ergonomic Segmented Pill Control for Top Navigation
 */
@Composable
private fun ErgonomicSegmentedBar(
    selectedMode: AppMode,
    onModeSelected: (AppMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF1D202A), RoundedCornerShape(20.dp)),
        color = Color(0xFF101217)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppMode.values().forEach { mode ->
                val isSelected = (selectedMode == mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            if (isSelected) Color(0xFF221A16)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) Color(0x66FF6D00) else Color.Transparent,
                            shape = RoundedCornerShape(17.dp)
                        )
                        .clickable { onModeSelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFFF9E40) else Color(0xFF757888),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.title,
                            fontFamily = PlusJakartaFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFFF2F2F6) else Color(0xFF757888)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimalist Compact Bluetooth Status Pill
 * Height: 28.dp, unobtrusive, clean, informative
 */
@Composable
private fun MiniBluetoothPill(
    state: PipHidState,
    onMakeDiscoverable: () -> Unit,
    onRetry: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (dotColor, statusText, action) = when (state) {
        is PipHidState.Connected -> {
            Triple(Color(0xFF10B981), "Подключено: ${state.hostName}", {})
        }
        is PipHidState.Ready -> {
            Triple(Color(0xFFFF6D00), "Готов к Mac • Нажмите для сопряжения", onMakeDiscoverable)
        }
        is PipHidState.Registering -> {
            Triple(Color(0xFFFF9E00), "Подключение HID профиля...", {})
        }
        is PipHidState.Idle -> {
            Triple(Color(0xFF6C6C78), "BLE остановлен • Нажмите для запуска", onRetry)
        }
        is PipHidState.Error -> {
            val isPerm = state.message.contains("разрешен", ignoreCase = true)
            Triple(Color(0xFFEF4444), "Bluetooth: ${state.message} • Повторить", if (isPerm) onRequestPermissions else onRetry)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = action)
            .testTag("status_bar"),
        color = Color(0xFF101217)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .alpha(if (state is PipHidState.Ready || state is PipHidState.Connected) pulseAlpha else 1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontFamily = PlusJakartaFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFC0C2CC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (state is PipHidState.Ready || state is PipHidState.Error) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = dotColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Minimalist Dual-Pad Mouse Buttons: Clean, Beautiful, ZERO Text!
 * Ergonomic split surface with central tactile notch lines and dual-level haptics
 */
@Composable
private fun MinimalDualMouseBar(
    onLeftClick: () -> Unit,
    onLeftDown: () -> Unit,
    onLeftUp: () -> Unit,
    onRightClick: () -> Unit,
    onRightDown: () -> Unit,
    onRightUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF1F222C), RoundedCornerShape(18.dp)),
        color = Color(0xFF0F1116),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Mouse Button (NO TEXT)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            if (isLeftPressed) listOf(Color(0x55FF6D00), Color(0x22FF6D00))
                            else listOf(Color(0xFF13151D), Color(0xFF0F1116))
                        )
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            isLeftPressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onLeftDown()
                            val up = waitForUpOrCancellation()
                            isLeftPressed = false
                            if (up != null) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onLeftClick()
                            }
                            onLeftUp()
                        }
                    }
                    .testTag("btn_left_click"),
                contentAlignment = Alignment.Center
            ) {
                // Tactile Orientation Micro-Dash (Zero Text)
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isLeftPressed) Color(0xFFFF6D00)
                            else Color(0xFF282B38)
                        )
                )
            }

            // Hairline Center Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color(0xFF232532))
            )

            // Right Mouse Button (NO TEXT)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            if (isRightPressed) listOf(Color(0x55FF9E00), Color(0x22FF9E00))
                            else listOf(Color(0xFF13151D), Color(0xFF0F1116))
                        )
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            isRightPressed = true
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onRightDown()
                            val up = waitForUpOrCancellation()
                            isRightPressed = false
                            if (up != null) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onRightClick()
                            }
                            onRightUp()
                        }
                    }
                    .testTag("btn_right_click"),
                contentAlignment = Alignment.Center
            ) {
                // Tactile Orientation Micro-Dash (Zero Text)
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isRightPressed) Color(0xFFFF9E00)
                            else Color(0xFF282B38)
                        )
                )
            }
        }
    }
}

/**
 * Minimalist Air Mouse Console
 */
@Composable
private fun AirMouseMinimalConsole(
    isEnabled: Boolean,
    hasGyroscope: Boolean,
    isMoving: Boolean,
    dx: Int,
    dy: Int,
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    onToggle: (Boolean) -> Unit,
    onRecenter: () -> Unit
) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Radar & Telemetry Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    width = 1.dp,
                    color = if (isEnabled) Color(0x44FF6D00) else Color(0xFF1C1E28),
                    shape = RoundedCornerShape(22.dp)
                ),
            color = Color(0xFF101217)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background concentric radar rings
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(
                            1.dp,
                            if (isEnabled) Color(0x22FF6D00) else Color(0xFF181A22),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(
                            1.dp,
                            if (isEnabled) Color(0x33FF6D00) else Color(0xFF1C1E26),
                            CircleShape
                        )
                )

                // Central Activator Ring & Power Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) Brush.radialGradient(listOf(Color(0xFFFF7A00), Color(0xFFCC3700)))
                                else Brush.radialGradient(listOf(Color(0xFF222430), Color(0xFF14161E)))
                            )
                            .border(
                                width = 2.dp,
                                color = if (isEnabled) Color(0xFFFFB300) else Color(0xFF323646),
                                shape = CircleShape
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onToggle(!isEnabled)
                            }
                            .testTag("btn_air_mouse_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Air Mouse Power",
                            tint = if (isEnabled) Color(0xFF090A0E) else Color(0xFF888894),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isEnabled) "Air Mouse Активен" else "Air Mouse Выключен",
                        fontFamily = PlusJakartaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isEnabled) Color(0xFFFF9E40) else Color(0xFF7A7D8E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isEnabled) "Наклоняйте смартфон в воздухе" else "Нажмите кнопку для запуска",
                        fontFamily = PlusJakartaFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFF6B6E7E)
                    )
                }

                // Live Telemetry Chip (Bottom of radar)
                if (isEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF090A0E))
                            .border(1.dp, Color(0x33FF6D00), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MotionPhotosOn,
                                contentDescription = null,
                                tint = if (isMoving) Color(0xFFFF9E00) else Color(0xFF6C6C78),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "dx: $dx  •  dy: $dy",
                                fontFamily = PlusJakartaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFF9E40)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sensitivity Control Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF1D202A), RoundedCornerShape(18.dp)),
            color = Color(0xFF101217)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Скорость курсора: ${sensitivity.roundToInt()}",
                        fontFamily = PlusJakartaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFFF2F2F6)
                    )
                    Text(
                        text = "Центрировать",
                        fontFamily = PlusJakartaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFFF9E40),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onRecenter()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Slider(
                    value = sensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 15.0f..80.0f,
                    steps = 64,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_sensitivity"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF6D00),
                        activeTrackColor = Color(0xFFFF6D00),
                        inactiveTrackColor = Color(0xFF222430)
                    )
                )
            }
        }
    }
}
