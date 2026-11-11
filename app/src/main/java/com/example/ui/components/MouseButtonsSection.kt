package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConstants
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MouseButtonsSection(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onButtonDown: (Byte) -> Unit,
    onButtonUp: (Byte) -> Unit,
    onCalibrateCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Quick tools bar (Center calibration & middle click)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27385A)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("btn_calibrate_center")
            ) {
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onCalibrateCenter()
                                waitForUpOrCancellation()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = "Сброс центра",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Сбросить дрейф / Центр",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Two large buttons: Left Click (0x01) and Right Click (0x02)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LargeMouseButton(
                title = "Левый клик",
                subtitle = "Left Click (0x01)",
                accentColor = CyanPrimary,
                testTag = "btn_left_click",
                onTap = onLeftClick,
                onDown = { onButtonDown(HidConstants.BUTTON_LEFT) },
                onUp = { onButtonUp(HidConstants.BUTTON_LEFT) },
                modifier = Modifier.weight(1f)
            )

            LargeMouseButton(
                title = "Правый клик",
                subtitle = "Right Click (0x02)",
                accentColor = NeonViolet,
                testTag = "btn_right_click",
                onTap = onRightClick,
                onDown = { onButtonDown(HidConstants.BUTTON_RIGHT) },
                onUp = { onButtonUp(HidConstants.BUTTON_RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LargeMouseButton(
    title: String,
    subtitle: String,
    accentColor: Color,
    testTag: String,
    onTap: () -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "buttonScale")

    Box(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.2f))
                    } else {
                        listOf(CardSurface, Color(0xFF0F172A))
                    }
                )
            )
            .border(
                width = if (isPressed) 2.dp else 1.dp,
                color = if (isPressed) accentColor else Color(0xFF334155),
                shape = RoundedCornerShape(20.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    val isRightClick = title.contains("ПКМ", ignoreCase = true) ||
                            title.contains("Правый", ignoreCase = true) ||
                            title.contains("Right", ignoreCase = true)
                    view.performHapticFeedback(
                        if (isRightClick) HapticFeedbackConstants.CONTEXT_CLICK
                        else HapticFeedbackConstants.VIRTUAL_KEY
                    )
                    onDown()
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onTap()
                    }
                    onUp()
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mouse,
                contentDescription = null,
                tint = if (isPressed) Color.White else accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.White else TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
