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

private const val INDICATOR_WIDTH_DP = 40f
private const val SIZE_REDUCTION_FACTOR = 0.8f
private const val ROOM_WIDTH_DP = 20f
private const val ROOM_HEIGHT_DP = ROOM_WIDTH_DP*2
private const val PADDING_DP = 4f
private const val MAX_POSITION = 13


/** Схематичный план этажа с выделением комнаты и этажа */
@Composable
fun MiniFloorPlan(
    floorPlan: ScheduleUiModel.FloorPlanUi,
    currentFloorColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val color = MaterialTheme.colorScheme.outlineVariant
    val fontSize = MaterialTheme.typography.labelSmall.fontSize



    val roomsByFloor = mapOf(
        1 to listOf(
            RoomData(1, 1, "103"),
            RoomData(3, 1, "104"),
            RoomData(4, 1, "105"),
            RoomData(5, 1, "106"),
            RoomData(6, 4, "Читательный зал"),
            RoomData(10, 1, "107"),
            RoomData(11, 1, "108"),
            RoomData(13, 1, "санитарно-бытовое помещение"),
            RoomData(1, 3, "Абонентский отдел", 2),
            RoomData(4, 2, "101",2),
            RoomData(6, 4, "Вестибюль",2),
            RoomData(10, 1, "100", 2),
            RoomData(11, 1, "99", 2),
            RoomData(12, 1, "98", 2),
            RoomData(13, 1, "97",2)
        ),
        2 to listOf(
            RoomData(1, 1, "207"),
            RoomData(3, 3, "208"),
            RoomData(6, 3, "209"),
            RoomData(9, 3, "210"),
            RoomData(13, 1, "Санитарно-бытовая комната"),
            RoomData(1, 1, "206", 2),
            RoomData(2, 3, "205", 2),
            RoomData(5, 3, "204", 2),
            RoomData(8, 1, "203", 2),
            RoomData(9, 3, "202", 2),
            RoomData(12, 1, "201", 2),
            RoomData(13, 1, "200", 2)
        ),
        3 to listOf(
            RoomData(1, 1, "307"),
            RoomData(3, 4, "308"),
            RoomData(7, 1, "309"),
            RoomData(8, 4, "310"),
            RoomData(13, 1, "Санитарно-бытовая комната"),
            RoomData(1, 1, "306", 2),
            RoomData(2, 3, "305", 2),
            RoomData(5, 3, "304", 2),
            RoomData(8, 1, "303", 2),
            RoomData(9, 3, "302", 2),
            RoomData(12, 1, "301", 2),
            RoomData(13, 1, "300", 2)
        ),
        4 to listOf(
            RoomData(1, 1, "413"),
            RoomData(3, 1, "414"),
            RoomData(4, 1, "415"),
            RoomData(5, 1, "416"),
            RoomData(6, 1, "417"),
            RoomData(7, 1, "418"),
            RoomData(8, 1, "419"),
            RoomData(9, 1, "420"),
            RoomData(10, 1, "421"),
            RoomData(11, 1, "422"),
            RoomData(13, 1, "Санитарно-бытовая комната"),
            RoomData(1, 1, "412", 2),
            RoomData(2, 1, "411", 2),
            RoomData(3, 1, "410", 2),
            RoomData(4, 1, "409", 2),
            RoomData(5, 1, "408", 2),
            RoomData(6, 1, "407", 2),
            RoomData(7, 1, "406", 2),
            RoomData(8, 1, "405", 2),
            RoomData(9, 1, "404", 2),
            RoomData(10, 1, "403", 2),
            RoomData(11, 2, "402", 2),
            RoomData(13, 1, "401", 2)
        )
    )


    val rooms = roomsByFloor[floorPlan.floor] ?: emptyList()

    // Рассчитываем размер Canvas: 13 позиций * (ширина + отступ) - 1 отступ
    val canvasHeightDp = ((ROOM_HEIGHT_DP * 2.5) +(PADDING_DP *4) )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp.dp)
            .padding(PADDING_DP.dp)
    ) {

        val indicatorWidthPx = INDICATOR_WIDTH_DP.dp.toPx()
        val paddingPx = PADDING_DP.dp.toPx()
        val baseRoomWidthPx = ROOM_WIDTH_DP.dp.toPx()
        val roomHeightPx = ROOM_HEIGHT_DP.dp.toPx()

        val totalSlotsWidthPx = (baseRoomWidthPx * MAX_POSITION) + (paddingPx * (MAX_POSITION - 1))
        val canvasWidthPx = totalSlotsWidthPx + indicatorWidthPx + paddingPx * 2
        val canvasHeightPx = ((roomHeightPx * 2.5f) +(paddingPx *4) )


        //val availableWidthPx = canvasWidthDp - INDICATOR_WIDTH_DP.dp.toPx() - (PADDING_DP.dp.toPx()*2)
        //val baseSlotWidthPx = (availableWidthPx / MAX_POSITION.dp.toPx()) * SIZE_REDUCTION_FACTOR.dp.toPx()

        rooms.forEach { room ->

            val roomWidthPx = ((baseRoomWidthPx * room.size) + (paddingPx*(room.size-1)))
            val roomHeightPx = ROOM_HEIGHT_DP.dp.toPx()
            val slotWidthPx = baseRoomWidthPx + paddingPx
            val x = indicatorWidthPx + paddingPx + (room.position - 1) * slotWidthPx - slotWidthPx

            // Позиция Y: ряд 1 = верхний, ряд 2 = нижний (с отступом между рядами)
            val y = if (room.row == 1) { paddingPx } else { paddingPx + roomHeightPx + roomHeightPx/2 }

            val isTarget = room.number == floorPlan.roomNumber

            drawRoundRect(
                color = if (isTarget) currentFloorColor else color,
                topLeft = Offset(x,y),
                size = Size(roomWidthPx, roomHeightPx),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = if (isTarget) Stroke(2.dp.toPx()) else Stroke(1.dp.toPx())
            )

            // Номер комнаты
            if (isTarget) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = floorPlan.roomNumber,
                    style = TextStyle(
                        color = currentFloorColor,
                        fontSize = fontSize
                    ),
                    topLeft = Offset(
                        x + roomWidthPx/3,
                        y + roomHeightPx/3
                    )
                )
            }
        }


        // Индикатор этажа (4 линии слева)

        for (floor in 0..3) {
            val y = (canvasHeightPx - roomHeightPx - ((floor - 1) * 50))
            drawLine(
                color = if (floor == floorPlan.floor) currentFloorColor
                else color,
                start = Offset(PADDING_DP, y),
                end = Offset(INDICATOR_WIDTH_DP, y),
                strokeWidth = if (floor == floorPlan.floor) 3.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )

        }
    }
}


/** Данные о комнате: номер, позиция в ряду (1..13), ряд (1=верхний, 2=нижний) */
private data class RoomData(
    val position: Int,
    val size: Int,
    val number: String,
    val row: Int = 1
)