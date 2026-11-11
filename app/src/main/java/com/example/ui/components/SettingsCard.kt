package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.AirMouseSettings
import com.example.data.model.GripMode
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceVariant
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsCard(
    settings: AirMouseSettings,
    onSensitivityChange: (Float) -> Unit,
    onDeadzoneChange: (Float) -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onInvertXChange: (Boolean) -> Unit,
    onInvertYChange: (Boolean) -> Unit,
    onGripModeChange: (GripMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF27385A), RoundedCornerShape(20.dp)),
        color = CardSurface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "НАСТРОЙКИ УПРАВЛЕНИЯ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Sensitivity slider (0.5x to 2.5x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Чувствительность",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "%.1fx".format(settings.sensitivity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }
            Slider(
                value = settings.sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 0.5f..2.5f,
                steps = 19, // 0.1 increments
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_sensitivity"),
                colors = SliderDefaults.colors(
                    thumbColor = CyanPrimary,
                    activeTrackColor = CyanPrimary,
                    inactiveTrackColor = CardSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Deadzone slider (0.01 to 0.15 rad/s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Мертвая зона (фильтр тремора)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Игнорирует микроколебания руки",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "%.2f рад/с".format(settings.deadzone),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }
            Slider(
                value = settings.deadzone,
                onValueChange = onDeadzoneChange,
                valueRange = 0.01f..0.15f,
                steps = 13,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_deadzone"),
                colors = SliderDefaults.colors(
                    thumbColor = CyanPrimary,
                    activeTrackColor = CyanPrimary,
                    inactiveTrackColor = CardSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. EMA Smoothing (0.1 to 0.8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Сглаживание EMA",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Фильтр низких частот движения",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "%.2f".format(settings.smoothingAlpha),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }
            Slider(
                value = settings.smoothingAlpha,
                onValueChange = onSmoothingChange,
                valueRange = 0.10f..0.80f,
                steps = 13,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_smoothing"),
                colors = SliderDefaults.colors(
                    thumbColor = CyanPrimary,
                    activeTrackColor = CyanPrimary,
                    inactiveTrackColor = CardSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Invert axes toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Инверт. X",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Switch(
                        checked = settings.invertX,
                        onCheckedChange = onInvertXChange,
                        modifier = Modifier.testTag("switch_invert_x"),
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Инверт. Y",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Switch(
                        checked = settings.invertY,
                        onCheckedChange = onInvertYChange,
                        modifier = Modifier.testTag("switch_invert_y"),
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Grip mode selector
            Text(
                text = "Положение смартфона",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = if (settings.gripMode == GripMode.POINTER) 0 else 1,
                containerColor = CardSurfaceVariant,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(
                            tabPositions[if (settings.gripMode == GripMode.POINTER) 0 else 1]
                        ),
                        color = CyanPrimary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = settings.gripMode == GripMode.POINTER,
                    onClick = { onGripModeChange(GripMode.POINTER) },
                    text = { Text("Как пульт ДУ", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = settings.gripMode == GripMode.PORTRAIT,
                    onClick = { onGripModeChange(GripMode.PORTRAIT) },
                    text = { Text("Вертикально", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }
}
