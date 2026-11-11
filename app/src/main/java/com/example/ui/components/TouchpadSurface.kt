package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PipHidController
import com.example.data.model.TouchpadSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

data class TouchIndicator(
    val id: Long,
    val position: Offset,
    val color: Color
)

@Composable
fun TouchpadSurface(
    pipHid: PipHidController,
    settings: TouchpadSettings,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var activeGestureLabel by remember { mutableStateOf("Коснитесь для управления") }
    var gestureColor by remember { mutableStateOf(Color(0xFFFF9E40)) }

    val activeTouches = remember { mutableStateListOf<TouchIndicator>() }

    // Tap detection state
    var touchStartTime by remember { mutableLongStateOf(0L) }
    var touchStartPosition by remember { mutableStateOf(Offset.Zero) }
    var maxPointersInGesture by remember { mutableStateOf(1) }
    var totalTravelDistance by remember { mutableStateOf(0f) }
    var lastSingleTapTime by remember { mutableLongStateOf(0L) }
    var isThreeFingerDragging by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF1E202B), RoundedCornerShape(22.dp))
            .testTag("touchpad_surface"),
        color = Color(0xFF101218),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pointers = event.changes
                            val activePointers = pointers.filter { it.pressed }
                            val pointerCount = activePointers.size

                            if (pointerCount > maxPointersInGesture) {
                                maxPointersInGesture = pointerCount
                            }

                            // Update visual feedback touch indicators with Solar Orange palette
                            activeTouches.clear()
                            activePointers.forEach { p ->
                                val color = when (maxPointersInGesture) {
                                    1 -> Color(0xFFFF6D00) // Neon Orange
                                    2 -> Color(0xFFFF9E00) // Amber Gold
                                    else -> Color(0xFFFF3D00) // Solar Flame
                                }
                                activeTouches.add(TouchIndicator(p.id.value, p.position, color))
                            }

                            when (pointerCount) {
                                0 -> {
                                    // All fingers lifted: evaluate taps
                                    val gestureDuration = System.currentTimeMillis() - touchStartTime

                                    if (isThreeFingerDragging) {
                                        // End 3-finger drag
                                        isThreeFingerDragging = false
                                        pipHid.setButtonUp(com.example.HidDescriptor.BUTTON_LEFT)
                                        activeGestureLabel = "Перетаскивание завершено"
                                        gestureColor = Color(0xFF10B981)
                                    } else if (gestureDuration < 240 && totalTravelDistance < 30f) {
                                        // TAP GESTURE
                                        when (maxPointersInGesture) {
                                            1 -> {
                                                val now = System.currentTimeMillis()
                                                if (now - lastSingleTapTime < 320) {
                                                    // Double tap
                                                    activeGestureLabel = "Двойной клик (ЛКМ)"
                                                    gestureColor = Color(0xFFFF6D00)
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    pipHid.doubleClickLeft()
                                                    lastSingleTapTime = 0L
                                                } else {
                                                    // Single tap
                                                    activeGestureLabel = "Один тап: Левый клик"
                                                    gestureColor = Color(0xFFFF6D00)
                                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                    pipHid.clickLeft()
                                                    lastSingleTapTime = now
                                                }
                                            }
                                            2 -> {
                                                // Two-finger tap -> Right Click (ПКМ)
                                                activeGestureLabel = "Два пальца: Правый клик (ПКМ)"
                                                gestureColor = Color(0xFFFF9E00)
                                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                                pipHid.clickRight()
                                            }
                                            3 -> {
                                                // Three-finger tap -> Middle Click
                                                activeGestureLabel = "Три пальца: Средний клик"
                                                gestureColor = Color(0xFFFF3D00)
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                pipHid.clickMiddle()
                                            }
                                        }
                                    }

                                    // Reset gesture counters after slight delay for visual label
                                    scope.launch {
                                        delay(800)
                                        if (activeTouches.isEmpty()) {
                                            activeGestureLabel = "Коснитесь для управления"
                                            gestureColor = Color(0xFF6C6C78)
                                        }
                                    }

                                    maxPointersInGesture = 0
                                    totalTravelDistance = 0f
                                }

                                1 -> {
                                    val pointer = activePointers[0]
                                    if (pointer.previousPressed.not()) {
                                        // Gesture start
                                        touchStartTime = System.currentTimeMillis()
                                        touchStartPosition = pointer.position
                                        maxPointersInGesture = 1
                                        totalTravelDistance = 0f
                                    } else {
                                        // 1 Finger move -> Mouse movement
                                        val deltaX = pointer.position.x - pointer.previousPosition.x
                                        val deltaY = pointer.position.y - pointer.previousPosition.y
                                        val dist = hypot(deltaX, deltaY)
                                        totalTravelDistance += dist

                                        activeGestureLabel = "1 палец: Движение курсора"
                                        gestureColor = Color(0xFFFF6D00)

                                        // Apply acceleration curve if enabled
                                        val accel = if (settings.pointerAcceleration && dist > 12f) {
                                            1f + (dist - 12f) * 0.04f
                                        } else 1f

                                        val dx = (deltaX * settings.touchpadSensitivity * accel).toInt()
                                        val dy = (deltaY * settings.touchpadSensitivity * accel).toInt()

                                        if (dx != 0 || dy != 0) {
                                            pipHid.mouse(0, dx, dy, 0)
                                        }
                                    }
                                }

                                2 -> {
                                    // 2 Fingers -> Scrolling
                                    val p1 = activePointers[0]
                                    val p2 = activePointers[1]

                                    val deltaY1 = p1.position.y - p1.previousPosition.y
                                    val deltaY2 = p2.position.y - p2.previousPosition.y
                                    val avgDeltaY = (deltaY1 + deltaY2) * 0.5f

                                    totalTravelDistance += kotlin.math.abs(avgDeltaY)

                                    if (kotlin.math.abs(avgDeltaY) > 2.0f) {
                                        activeGestureLabel = "2 пальца: Прокрутка (Scroll)"
                                        gestureColor = Color(0xFFFF9E00)

                                        val direction = if (settings.naturalScrolling) -1 else 1
                                        val wheel = (avgDeltaY * settings.scrollSensitivity * direction * 0.4f).toInt()
                                        if (wheel != 0) {
                                            pipHid.scroll(wheel)
                                        }
                                    }
                                }

                                3 -> {
                                    // 3 Fingers -> Drag & Drop
                                    val p1 = activePointers[0]
                                    val deltaX = p1.position.x - p1.previousPosition.x
                                    val deltaY = p1.position.y - p1.previousPosition.y
                                    val dist = hypot(deltaX, deltaY)
                                    totalTravelDistance += dist

                                    if (!isThreeFingerDragging && totalTravelDistance > 15f) {
                                        isThreeFingerDragging = true
                                        pipHid.setButtonDown(com.example.HidDescriptor.BUTTON_LEFT)
                                    }

                                    if (isThreeFingerDragging) {
                                        activeGestureLabel = "3 пальца: Перетаскивание (Drag)"
                                        gestureColor = Color(0xFFFF3D00)

                                        val dx = (deltaX * settings.touchpadSensitivity).toInt()
                                        val dy = (deltaY * settings.touchpadSensitivity).toInt()
                                        if (dx != 0 || dy != 0) {
                                            pipHid.mouse(com.example.HidDescriptor.BUTTON_LEFT, dx, dy, 0)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Background grid pattern and touches canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Subtle warm orange dotted trackpad texture
                val dotColor = Color(0x22FF6D00)
                val step = 40f
                var x = step
                while (x < w) {
                    var y = step
                    while (y < h) {
                        drawCircle(dotColor, radius = 1.5f, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }

                // Render glowing finger touches
                activeTouches.forEach { touch ->
                    drawCircle(
                        color = touch.color.copy(alpha = 0.25f),
                        radius = 48f,
                        center = touch.position
                    )
                    drawCircle(
                        color = touch.color.copy(alpha = 0.5f),
                        radius = 24f,
                        center = touch.position
                    )
                    drawCircle(
                        color = touch.color,
                        radius = 8f,
                        center = touch.position
                    )
                }
            }

            // Top HUD status indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF09090D).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gestureColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (activeTouches.size > 1) Icons.Default.PanTool else Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = gestureColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeGestureLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = gestureColor
                        )
                    }
                }
            }

            // Bottom gestures cheat-sheet hint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "1 палец: Курсор • Тап: ЛКМ • 2 пальца: ПКМ / Скролл • 3 пальца: Drag",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF555562),
                    fontSize = 11.sp
                )
            }
        }
    }
}
