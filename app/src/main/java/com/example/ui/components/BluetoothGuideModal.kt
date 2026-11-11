package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangePrimaryLight
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothGuideModal(
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlateBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Справка и управление",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pip Air Mouse • Руководство пользователя",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangePrimaryLight
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Bluetooth Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onMakeDiscoverable()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сделать видимым", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onOpenBluetoothSettings()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimaryLight),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bluetooth вкл.", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Connection Steps
            GuideSectionHeader(title = "ПОДКЛЮЧЕНИЕ К MAC / ПК")
            Spacer(modifier = Modifier.height(8.dp))

            GuideStepCard(
                step = "1",
                title = "Включите Bluetooth на Mac или ПК",
                description = "На Mac: Системные настройки → Bluetooth. На ПК: Параметры → Bluetooth и устройства."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GuideStepCard(
                step = "2",
                title = "Нажмите «Сделать видимым»",
                description = "Телефон перейдет в режим сопряжения на 300 секунд и появится в списке доступных устройств."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GuideStepCard(
                step = "3",
                title = "Выберите «Pip Air Mouse»",
                description = "Нажмите «Подключить» на компьютере. Готово! Устройство распознается как беспроводная мышь."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Touchpad Gestures
            GuideSectionHeader(title = "ЖЕСТЫ ТРЕКПАДА")
            Spacer(modifier = Modifier.height(8.dp))

            GestureInfoCard(
                icon = Icons.Default.TouchApp,
                title = "1 палец: Перемещение курсора",
                subtitle = "Плавное позиционирование стрелки мыши по экрану с ускорением."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GestureInfoCard(
                icon = Icons.Default.Mouse,
                title = "1 тап: Левый клик (ЛКМ)",
                subtitle = "Быстрое касание одним пальцем нажимает левую кнопку."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GestureInfoCard(
                icon = Icons.Default.Mouse,
                title = "2 пальца (тап): Правый клик (ПКМ)",
                subtitle = "Касание двумя пальцами одновременно открывает контекстное меню."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GestureInfoCard(
                icon = Icons.Default.TouchApp,
                title = "2 пальца (свайп): Прокрутка (Скролл)",
                subtitle = "Движение двумя пальцами вверх/вниз плавно прокручивает страницу или документ."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Air Mouse Gyroscope
            GuideSectionHeader(title = "РЕЖИМ AIR MOUSE (ГИРОСКОП)")
            Spacer(modifier = Modifier.height(8.dp))

            GestureInfoCard(
                icon = Icons.Default.Adjust,
                title = "Управление наклоном смартфона",
                subtitle = "Включите переключатель Air Mouse. Наклоняйте телефон в воздухе — гироскоп преобразует угловую скорость в движение курсора."
            )
            Spacer(modifier = Modifier.height(8.dp))
            GestureInfoCard(
                icon = Icons.Default.Adjust,
                title = "Фильтр тремора рук и чувствительность",
                subtitle = "Во вкладке «Настройки» можно отрегулировать мертвую зону (игнорирование микро-дрожания кисти) и скорость отклика."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Bottom Buttons
            GuideSectionHeader(title = "НИЖНИЕ КЛАВИШИ МЫШИ")
            Spacer(modifier = Modifier.height(8.dp))

            GestureInfoCard(
                icon = Icons.Default.Mouse,
                title = "Минималистичные сенсорные клавиши",
                subtitle = "Левая нижняя зона отвечает за Левый клик (ЛКМ), правая — за Правый клик (ПКМ). Обе клавиши оснащены четким тактильным виброоткликом клика."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = OrangePrimaryLight,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun GuideStepCard(step: String, title: String, description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x33FF6D00), RoundedCornerShape(14.dp)),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OrangePrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GestureInfoCard(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF20222C), RoundedCornerShape(14.dp)),
        color = CardSurface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B1D26)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
