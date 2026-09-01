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
import androidx.compose.ui.unit.dp
import com.example.bgtischedule.model.ClassroomDirectory
import com.example.bgtischedule.model.ScheduleUiModel

private const val INDICATOR_WIDTH_DP = 30f
private const val PADDING = 4f
private const val MAX_POSITION = 13


/** Схематичный план этажа с выделением комнаты и этажа */
@Composable
fun MiniFloorPlan(
    floorPlan: ScheduleUiModel.FloorPlanUi,
    currentFloorColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier.fillMaxWidth().wrapContentHeight()
) {
    val textMeasurer = rememberTextMeasurer()

    val color = MaterialTheme.colorScheme.outlineVariant
    val fontSize = MaterialTheme.typography.labelSmall.fontSize


    // Вычлинение номера корпуса (1-й символ преобразуется в число)  //Добавить проверку?
    var body = extractBuildingNumber(floorPlan.building)
    if (body !in 1..3) return

    val rooms = ClassroomDirectory.getRoomsForFloor(body, floorPlan.floor)

// ✅ ПРОВЕРКА: есть ли искомая аудитория в корпусе?
    val roomExists = rooms.any { it.number == floorPlan.roomNumber }

// Если корпус не распознан ИЛИ аудитория не найдена — не рисуем карту
    val shouldDraw = body in 1..3 && roomExists

    if (shouldDraw) {
        // Получаем список комнат ТОЛЬКО для текущего этажа

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Максимальное количество ячеек в высоту
            val maxY = rooms.maxOfOrNull { it.y + it.sizeY - 1 } ?: 2
            // Ширина_ячейки_Dp =  ширина   -  ширина_индикатора     + (отступ        * (3 + количество_позиций)) / количество_позиций
            val baseRoomWidthDp =
                (maxWidth - (INDICATOR_WIDTH_DP.dp + (PADDING.dp * (3 + MAX_POSITION)))) / MAX_POSITION
            // Высота Ячейки в Px
            val baseRoomHeightPx = baseRoomWidthDp
            // Высота холста = аысота ячейки    * количество_позиций + отсступ     * (количчество позиций) + 1
            val canvasHeight =
                baseRoomHeightPx * maxY.toFloat() + (PADDING.dp * (maxY.toFloat() + 1)) + PADDING.dp * 2

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .padding(PADDING.dp)
            ) {
                //drawLine(color, Offset(0.dp.toPx(), 0.dp.toPx()), Offset(maxWidth.toPx(), 0.dp.toPx()), 2f)
                //drawLine(color, Offset(0.dp.toPx(), 0.dp.toPx()), Offset(0.dp.toPx(), canvasHeight.toPx()),2f)
                val paddingPx = PADDING.dp.toPx()
                val indicatorWidthPx = INDICATOR_WIDTH_DP.dp.toPx()
                val baseRoomWidthPx = baseRoomWidthDp.toPx()
                val baseRoomHeightPx = baseRoomWidthPx

                rooms.forEach { room ->

                    // Ширина ячейки с отступом
                    val slotWidthPx = baseRoomWidthPx + paddingPx
                    // Длина ячейки с отступом
                    val slotHeightPx = baseRoomHeightPx + paddingPx
                    // Ширина_кабинета = (ширина_ячейки * количество_ячеек) - отступ
                    val roomWidthPx = (slotWidthPx * room.sizeX) - paddingPx
                    // Длина_кабинета = (длина_ячейки * количество_ячеек) - отступ
                    val roomHeightPx = (slotHeightPx * room.sizeY) - paddingPx
                    // Позиция кабинета = ширина_индикатора + (координаты_широты_кабинета) * количество_ячеек - сдвиг
                    val x = indicatorWidthPx + ((room.x - 1) * slotWidthPx)
                    //val x = indicatorWidthPx + paddingPx + ((room.x - 1) * slotWidthPx - slotWidthPx)
                    val y = paddingPx + (slotHeightPx * (room.y - 1))

                    // Целевой кабинет
                    val isTarget = room.number == floorPlan.roomNumber

                    drawRoundRect(
                        color = if (isTarget) currentFloorColor else color,
                        topLeft = Offset(x, y),
                        size = Size(
                            roomWidthPx,
                            roomHeightPx
                        ),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = if (isTarget) Stroke(2.dp.toPx()) else Stroke(1.dp.toPx())
                    )

                    // Номер комнаты
                    if (isTarget) {
                        val textLayoutResult = textMeasurer.measure(
                            text = floorPlan.roomNumber,
                            style = TextStyle(color = currentFloorColor, fontSize = fontSize)
                        )
                        val textW = textLayoutResult.size.width.toFloat()
                        val textH = textLayoutResult.size.height.toFloat()

                        // Небольшой внутренний отступ, чтобы текст не прилипал к границам
                        val hPadding = 4.dp.toPx()
                        val vPadding = 4.dp.toPx()

                        val fitsWidth  = textW <= (roomWidthPx  - hPadding * 2)
                        val fitsHeight = textH <= (roomHeightPx - vPadding * 2)

                        if (fitsWidth && fitsHeight) {
                            // ✅ Центрирование: (размер контейнера - размер текста) / 2
                            val textX = x + (roomWidthPx - textW) / 2f
                            val textY = y + (roomHeightPx - textH) / 2f

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(textX, textY)
                            )
                        }
                    }
                }


                // Индикатор этажа

                for (floor in 0..if (body == 2) 3 else 1) {

                    val y =
                        (canvasHeight - (PADDING.dp * 3) - (floor * ((baseRoomHeightPx / 2) - (PADDING / 2))).dp)
                    drawLine(
                        color = if (floor == floorPlan.floor - 1) currentFloorColor
                        else color,
                        start = Offset(PADDING.dp.toPx(), y.toPx()),
                        end = Offset((INDICATOR_WIDTH_DP - PADDING).dp.toPx(), y.toPx()),
                        strokeWidth = if (floor == floorPlan.floor - 1) 3.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                }
            }
        }
    }
}

/** Преобразует "2 корпус" → 2 */
private fun extractBuildingNumber(building: String): Int = when {
    building.contains("1") -> 1
    building.contains("2") -> 2
    building.contains("3") -> 3
    else -> 0
}


/** Данные о комнате: позиция в ряду (1..13), позиция в столбце (1-5), ширина кабинета, высота кабинета, название кабинета */
private data class RoomData(
    val x: Float,
    val y: Float,
    val sizeX: Float,
    val sizeY: Float,
    val number: String
)