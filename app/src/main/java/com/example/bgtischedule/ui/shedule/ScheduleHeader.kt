package com.example.bgtischedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TopBarSideSlotWidth = 70.dp

/**
 * Верхняя панель экрана расписания:
 * слева — обновление, по центру — заголовок, справа — время;
 * под ней по центру — диапазон недели.
 */
@Composable
fun SchedulePageHeader(
    title: String,
    weekRange: String,
    onRefresh: () -> Unit,
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTime = LocalTime.now()
        }
    }

    val timeText = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val animatedAlpha by animateFloatAsState(
        targetValue = if (currentTime.second % 2 == 0) 1f else 0.8f,
        label = "timeBlink"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(TopBarSideSlotWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(TopBarSideSlotWidth)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить расписание",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Box(
                modifier = Modifier.width(TopBarSideSlotWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = currentTimeColor.copy(alpha = animatedAlpha),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        if (weekRange.isNotBlank()) {
            Text(
                text = weekRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
