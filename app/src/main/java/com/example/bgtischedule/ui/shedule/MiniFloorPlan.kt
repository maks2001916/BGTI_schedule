package com.example.bgtischedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bgtischedule.model.ScheduleUiModel

/** Схематичный план этажа 2×2 с выделением комнаты и этажа */
@Composable
fun MiniFloorPlan(
    floorPlan: ScheduleUiModel.FloorPlanUi,
    currentFloorColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val color = MaterialTheme.colorScheme.outlineVariant
    val fontSize = MaterialTheme.typography.labelSmall.fontSize

    // Позиции 4 комнат на плане 2×2
    val rooms = listOf(
        RoomGrid(1, 1, Offset(10.toFloat(), 10.toFloat())),   // левая  верхняя
        RoomGrid(1, 2, Offset(120.toFloat(), 10.toFloat())),   // правая верхняя
        RoomGrid(2, 1, Offset(10.toFloat(), 80.toFloat())),   // левая  нижняя
        RoomGrid(2, 2, Offset(120.toFloat(), 80.toFloat()))    // правая нижняя
    )


    Canvas(
        modifier = modifier
            .size(80.dp)
            .padding(4.dp)
    ) {
        val roomSize = Size(35.dp.toPx(), 20.dp.toPx())

        // 🏢 Рисуем 4 комнаты (схематично)
        rooms.forEach { room ->
            val isTarget = room.floor == floorPlan.floor &&
                    room.position == (if (floorPlan.floor < 0.5f) 1 else 2)

            drawRoundRect(
                color = if (isTarget) currentFloorColor
                else color,
                topLeft = room.offset,
                size = roomSize,
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = if (isTarget) Stroke(2.dp.toPx()) else Stroke(1.dp.toPx())
            )

            // Номер комнаты (если целевая)
            if (isTarget) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = floorPlan.roomNumber,
                    style = TextStyle(
                        color = currentFloorColor,
                        fontSize = fontSize
                    ),
                    topLeft = room.offset + Offset(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }

        // Индикатор этажа (4 линии слева)
        val indicatorX = -40f
        for (floor in 1..4) {
            val y = 5.dp.toPx() + (floor - 1) * 15.dp.toPx()
            drawLine(
                color = if (floor == floorPlan.floor) currentFloorColor
                else color,
                start = Offset(indicatorX, y),
                end = Offset(indicatorX + 12.dp.toPx(), y),
                strokeWidth = if (floor == floorPlan.floor) 3.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}


private data class RoomGrid(val floor: Int, val position: Int, val offset: Offset)