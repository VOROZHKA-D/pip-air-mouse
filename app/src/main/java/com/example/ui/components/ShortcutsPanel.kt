package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HidDescriptor
import com.example.PipHidController

@Composable
fun ShortcutsPanel(
    pipHid: PipHidController,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragLocked by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2E2E3C), RoundedCornerShape(22.dp))
            .testTag("shortcuts_panel"),
        color = Color(0xFF13131A),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "ПАНЕЛЬ МАКРОСОВ И БЫСТРЫХ ДЕЙСТВИЙ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888894),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutTile(
                    title = "Двойной клик",
                    subtitle = "2x ЛКМ",
                    icon = Icons.Default.Mouse,
                    color = Color(0xFFFF6D00),
                    onClick = { pipHid.doubleClickLeft() },
                    modifier = Modifier.weight(1f)
                )

                ShortcutTile(
                    title = "Колёсико",
                    subtitle = "Middle Click",
                    icon = Icons.Default.CropSquare,
                    color = Color(0xFFFF9E00),
                    onClick = { pipHid.clickMiddle() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Drag Lock & Recenter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutTile(
                    title = if (isDragLocked) "ЛКМ Зажата" else "Залипание ЛКМ",
                    subtitle = if (isDragLocked) "Нажмите отпустить" else "Drag Lock",
                    icon = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    color = if (isDragLocked) Color(0xFFFF3D00) else Color(0xFFFF9E40),
                    onClick = {
                        isDragLocked = !isDragLocked
                        if (isDragLocked) {
                            pipHid.setButtonDown(HidDescriptor.BUTTON_LEFT)
                        } else {
                            pipHid.setButtonUp(HidDescriptor.BUTTON_LEFT)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                ShortcutTile(
                    title = "Центровка",
                    subtitle = "Сброс дрейфа",
                    icon = Icons.Default.Adjust,
                    color = Color(0xFFFF6D00),
                    onClick = onRecenter,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Fast Scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutTile(
                    title = "Скролл вверх",
                    subtitle = "Page Up",
                    icon = Icons.Default.ArrowUpward,
                    color = Color(0xFFF59E0B),
                    onClick = { pipHid.scroll(8) },
                    modifier = Modifier.weight(1f)
                )

                ShortcutTile(
                    title = "Скролл вниз",
                    subtitle = "Page Down",
                    icon = Icons.Default.ArrowDownward,
                    color = Color(0xFFF59E0B),
                    onClick = { pipHid.scroll(-8) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF262634), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF181822)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4F4F6)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888894),
                    fontSize = 11.sp
                )
            }
        }
    }
}
