package com.example.ui.components

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
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VirtualCursorPad(
    cursorNormalized: Offset,
    isAirMouseActive: Boolean,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF27385A), RoundedCornerShape(20.dp))
            .testTag("virtual_cursor_pad"),
        color = CardSurface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ВИРТУАЛЬНЫЙ ТЕСТОВЫЙ ЭКРАН",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAirMouseActive) "Курсор отслеживает наклон телефона"
                        else "Включите Air Mouse для движения курсора",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onRecenter,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = "В центр",
                        tint = CyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas field representing computer screen space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070B14))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw subtle grid lines
                    val gridColor = Color(0xFF131D31)
                    val cols = 6
                    val rows = 4
                    for (i in 1 until cols) {
                        val x = w * (i.toFloat() / cols)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    }
                    for (j in 1 until rows) {
                        val y = h * (j.toFloat() / rows)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    // Center crosshair marker
                    val centerX = w * 0.5f
                    val centerY = h * 0.5f
                    drawLine(
                        Color(0xFF334155),
                        Offset(centerX - 12f, centerY),
                        Offset(centerX + 12f, centerY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        Color(0xFF334155),
                        Offset(centerX, centerY - 12f),
                        Offset(centerX, centerY + 12f),
                        strokeWidth = 1.5f
                    )

                    // Draw animated cursor pointer
                    val cursorX = cursorNormalized.x * w
                    val cursorY = cursorNormalized.y * h

                    // Outer glowing ring
                    drawCircle(
                        color = if (isAirMouseActive) CyanPrimary.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.2f),
                        radius = 16f,
                        center = Offset(cursorX, cursorY)
                    )

                    // Inner crisp circle
                    drawCircle(
                        color = if (isAirMouseActive) CyanPrimary else Color.Gray,
                        radius = 6f,
                        center = Offset(cursorX, cursorY)
                    )

                    // Crosshair guides on cursor
                    if (isAirMouseActive) {
                        drawLine(
                            CyanPrimary.copy(alpha = 0.6f),
                            Offset(cursorX - 10f, cursorY),
                            Offset(cursorX + 10f, cursorY),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            CyanPrimary.copy(alpha = 0.6f),
                            Offset(cursorX, cursorY - 10f),
                            Offset(cursorX, cursorY + 10f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
