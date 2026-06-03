package com.example.bgtischedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleHeader(
    weekRange: String,  // "13–19 апреля"
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {}
) {
    // Текущее время с анимацией обновления
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    // Обновляем время каждую минуту
    LaunchedEffect(Unit) {
        snapshotFlow { Unit }
            .onEach { currentTime = LocalTime.now() }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .collect {}
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка обновления
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Обновить расписание",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        // Диапазон недели (по центру слева)
        Text(
            text = weekRange,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Текущее время (справа, акцентным цветом)
        val timeText = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val animatedAlpha by animateFloatAsState(
            targetValue = if (currentTime.second % 2 == 0) 1f else 0.8f,
            label = "timeBlink"
        )

        Text(
            text = timeText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = currentTimeColor.copy(alpha = animatedAlpha),
            modifier = Modifier
                .padding(start = 4.dp).wrapContentWidth()
        )
    }
}