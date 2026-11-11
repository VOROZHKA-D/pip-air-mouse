package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HidDescriptor
import com.example.PipHidController

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingTabletSurface(
    pipHid: PipHidController,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentStrokeWidth by remember { mutableFloatStateOf(4f) }
    var penPressure by remember { mutableFloatStateOf(1f) }
    var isStylus by remember { mutableStateOf(false) }

    var prevX by remember { mutableFloatStateOf(0f) }
    var prevY by remember { mutableFloatStateOf(0f) }
    var strokeColor by remember { mutableStateOf(Color(0xFFFF6D00)) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2E2E3C), RoundedCornerShape(22.dp))
            .testTag("drawing_tablet_surface"),
        color = Color(0xFF0F0F14),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161E))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = null,
                        tint = strokeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Графический планшет",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6)
                        )
                        Text(
                            text = if (isStylus) "Стилус (S-Pen) • Давление: ${(penPressure * 100).toInt()}%"
                            else "Сенсорный ввод • Рисует на Mac",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888894),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Color selectors
                    listOf(
                        Color(0xFFFF6D00), // Solar Orange
                        Color(0xFFFFB300), // Amber Gold
                        Color(0xFFFF3D00), // Flame Coral
                        Color(0xFFFFFFFF), // Crisp White
                        Color(0xFF00E5FF)  // Cyber Cyan
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (strokeColor == color) 2.dp else 0.dp,
                                    Color.White,
                                    CircleShape
                                )
                                .pointerInteropFilter {
                                    if (it.action == MotionEvent.ACTION_DOWN) {
                                        strokeColor = color
                                    }
                                    true
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { paths.clear() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Очистить холст",
                            tint = Color(0xFF888894)
                        )
                    }
                }
            }

            // Canvas drawing surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInteropFilter { motionEvent ->
                        val x = motionEvent.x
                        val y = motionEvent.y
                        penPressure = motionEvent.pressure.coerceIn(0.1f, 1.0f)
                        isStylus = motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS

                        when (motionEvent.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                prevX = x
                                prevY = y
                                val newPath = Path().apply { moveTo(x, y) }
                                currentPath = newPath
                                // Hold Left Click to draw in desktop application (Photoshop, Figma, Notes)
                                pipHid.setButtonDown(HidDescriptor.BUTTON_LEFT)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = (x - prevX).toInt().coerceIn(-127, 127)
                                val dy = (y - prevY).toInt().coerceIn(-127, 127)

                                currentPath?.lineTo(x, y)
                                prevX = x
                                prevY = y

                                if (dx != 0 || dy != 0) {
                                    pipHid.mouse(HidDescriptor.BUTTON_LEFT, dx, dy, 0)
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                currentPath?.let {
                                    val finalWidth = if (isStylus) 2f + penPressure * 8f else 4f
                                    paths.add(DrawingPath(it, strokeColor, finalWidth))
                                }
                                currentPath = null
                                // Release Left Click
                                pipHid.setButtonUp(HidDescriptor.BUTTON_LEFT)
                            }
                        }
                        true
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw saved paths
                    paths.forEach { drawItem ->
                        drawPath(
                            path = drawItem.path,
                            color = drawItem.color,
                            style = Stroke(
                                width = drawItem.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw active path
                    currentPath?.let { p ->
                        val strokeWidth = if (isStylus) 2f + penPressure * 8f else currentStrokeWidth
                        drawPath(
                            path = p,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (paths.isEmpty() && currentPath == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Рисуйте пальцем или стилусом\nШтрих повторяется на экране Mac",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF444452),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
