package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TouchpadSettings

@Composable
fun PrecisionSettingsCard(
    settings: TouchpadSettings,
    onSettingsChange: (TouchpadSettings) -> Unit,
    gyroSensitivity: Float,
    onGyroSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2E2E3C), RoundedCornerShape(22.dp)),
        color = Color(0xFF13131A),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "НАСТРОЙКИ ТОЧНОСТИ И ПЛАВНОСТИ",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888894),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Touchpad Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Скорость трекпада",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = "Чувствительность перемещения пальцем",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888894)
                    )
                }
                Text(
                    text = "%.1fx".format(settings.touchpadSensitivity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6D00)
                )
            }

            Slider(
                value = settings.touchpadSensitivity,
                onValueChange = { onSettingsChange(settings.copy(touchpadSensitivity = it)) },
                valueRange = 0.5f..3.0f,
                steps = 24,
                modifier = Modifier.fillMaxWidth().testTag("slider_touch_speed"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6D00),
                    activeTrackColor = Color(0xFFFF6D00),
                    inactiveTrackColor = Color(0xFF262634)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Pointer Acceleration toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ускорение указателя",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = if (settings.pointerAcceleration) "Динамическое (быстрый свайп ускоряет курсор)"
                        else "Линейное 1:1 (как на графическом планшете)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888894)
                    )
                }
                Switch(
                    checked = settings.pointerAcceleration,
                    onCheckedChange = { onSettingsChange(settings.copy(pointerAcceleration = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF6D00),
                        checkedTrackColor = Color(0xFF4E2000)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Scroll Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Скорость скролла (2 пальца)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = "Шаг прокрутки страниц на Mac",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888894)
                    )
                }
                Text(
                    text = "%.1fx".format(settings.scrollSensitivity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9E00)
                )
            }

            Slider(
                value = settings.scrollSensitivity,
                onValueChange = { onSettingsChange(settings.copy(scrollSensitivity = it)) },
                valueRange = 0.5f..3.0f,
                steps = 24,
                modifier = Modifier.fillMaxWidth().testTag("slider_scroll_speed"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9E00),
                    activeTrackColor = Color(0xFFFF9E00),
                    inactiveTrackColor = Color(0xFF262634)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Natural Scrolling toggle (macOS style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Натуральная прокрутка (macOS)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = if (settings.naturalScrolling) "Свайп вверх двигает контент вниз (как на трекпаде Mac)"
                        else "Классическая прокрутка (Windows колесико)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888894)
                    )
                }
                Switch(
                    checked = settings.naturalScrolling,
                    onCheckedChange = { onSettingsChange(settings.copy(naturalScrolling = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF9E00),
                        checkedTrackColor = Color(0xFF382300)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Gyro Air Mouse Sensitivity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Чувствительность гироскопа (Air Mouse)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = "Скорость движения при наклоне телефона в воздухе",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888894)
                    )
                }
                Text(
                    text = "${gyroSensitivity.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6D00)
                )
            }

            Slider(
                value = gyroSensitivity,
                onValueChange = onGyroSensitivityChange,
                valueRange = 15.0f..80.0f,
                steps = 64,
                modifier = Modifier.fillMaxWidth().testTag("slider_gyro_speed"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF6D00),
                    activeTrackColor = Color(0xFFFF6D00),
                    inactiveTrackColor = Color(0xFF262634)
                )
            )
        }
    }
}
