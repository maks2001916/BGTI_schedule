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
    var drawable = true

    val roomsByFloor1 = mapOf(
        1 to listOf(
            RoomData(6f, 1f, 2f, 1f, "Кабинет"),
            RoomData(1f, 2f, 2f,2f, "Кабинет директора"),
            RoomData(1f, 4f, 2f, 1f,  "Кабинет отдыха"),
            RoomData(3f, 2f, 1f, 3f, "Приёмная"),
            RoomData(4f, 2f, 2f, 2f, "Деканат"),
            RoomData(6f, 2f, 2f, 3f, "Санитарно-бытовая комната"),
            RoomData(9f, 2f, 3f, 2f, "16"),
            RoomData(1f, 5f, 4f, 3f, "11"),
            RoomData(5f, 5f, 4f, 3f, "Фае"),
            RoomData(9f, 4f, 3f, 3f, "17")
        ),
        2 to listOf(
            RoomData(4f, 1f, 2f, 1f, "Кафедра"),
            RoomData(1f, 2f, 4f, 2f, "Компьютерный класс"),
            RoomData(5f, 2f, 1f, 1f, "Инвентарное помещение"),
            RoomData(7f, 2f, 3f, 2f, "21"),
            RoomData(1f, 4f, 3f, 3f, "24"),
            RoomData(4f, 4f, 3f, 3f, "23"),
            RoomData(7f, 4f, 3f, 3f, "22")
        )
    )

    val roomsByFloor2 = mapOf(
        1 to listOf(
            RoomData(1f, 1f, 1f, 1.5f, "103"),
            RoomData(3f, 1f, 1f,1.5f, "104"),
            RoomData(4f, 1f, 1f, 1.5f,  "105"),
            RoomData(5f, 1f, 1f, 1.5f, "106"),
            RoomData(6f, 1f, 4f, 1.5f, "читальный зал библиотеки"),
            RoomData(10f, 1f, 1f, 1.5f, "107"),
            RoomData(11f, 1f, 1f, 1.5f, "108"),
            RoomData(13f, 1f, 1f, 1.5f, "санитарно-бытовое помещение"),
            RoomData(1f, 3f, 3f, 1.5f, "Абонентский отдел"),
            RoomData(4f, 3f, 2f, 1.5f, "101"),
            RoomData(6f, 3f, 4f, 1.5f, "Вестибюль"),
            RoomData(10f, 3f, 1f, 1.5f, "100"),
            RoomData(11f, 3f, 1f, 1.5f, "99"),
            RoomData(12f, 3f, 1f, 1.5f, "98"),
            RoomData(13f, 3f, 1f, 1.5f, "97")
        ),
        2 to listOf(
            RoomData(1f, 1f, 1f, 1.5f, "207"),
            RoomData(3f, 1f, 3f, 1.5f, "208"),
            RoomData(6f, 1f, 3f, 1.5f, "209"),
            RoomData(9f, 1f, 3f, 1.5f, "210"),
            RoomData(13f, 1f, 1f, 1.5f, "Санитарно-бытовая комната"),
            RoomData(1f, 3f, 1f, 1.5f, "206"),
            RoomData(2f, 3f, 3f, 1.5f, "205"),
            RoomData(5f, 3f, 3f, 1.5f, "204"),
            RoomData(8f, 3f, 1f, 1.5f, "203"),
            RoomData(9f, 3f, 3f, 1.5f, "202"),
            RoomData(12f, 3f, 1f, 1.5f, "201"),
            RoomData(13f, 3f, 1f, 1.5f, "200")
        ),
        3 to listOf(
            RoomData(1f, 1f, 1f, 1.5f, "307"),
            RoomData(3f, 1f, 4f, 1.5f, "308"),
            RoomData(7f, 1f, 1f, 1.5f, "309"),
            RoomData(8f, 1f, 4f, 1.5f, "310"),
            RoomData(13f, 1f, 1f, 1.5f, "Санитарно-бытовая комната"),
            RoomData(1f, 3f, 1f, 1.5f, "306"),
            RoomData(2f, 3f, 3f, 1.5f, "305"),
            RoomData(5f, 3f, 3f, 1.5f, "304"),
            RoomData(8f, 3f, 1f, 1.5f, "303"),
            RoomData(9f, 3f, 3f, 1.5f, "302"),
            RoomData(12f, 3f, 1f, 1.5f, "301"),
            RoomData(13f, 3f, 1f, 1.5f, "300")
        ),
        4 to listOf(
            RoomData(1f, 1f, 1f, 1.5f, "413"),
            RoomData(3f, 1f, 1f, 1.5f, "414"),
            RoomData(4f, 1f, 1f, 1.5f, "415"),
            RoomData(5f, 1f, 1f, 1.5f, "416"),
            RoomData(6f, 1f, 1f, 1.5f, "417"),
            RoomData(7f, 1f, 1f, 1.5f, "418"),
            RoomData(8f, 1f, 1f, 1.5f, "419"),
            RoomData(9f, 1f, 1f, 1.5f, "420"),
            RoomData(10f, 1f, 1f, 1.5f, "421"),
            RoomData(11f, 1f, 1f, 1.5f, "422"),
            RoomData(13f, 1f, 1f, 1.5f, "Санитарно-бытовая комната"),
            RoomData(1f, 3f, 1f, 1.5f, "412"),
            RoomData(2f, 3f, 1f, 1.5f, "411"),
            RoomData(3f, 3f, 1f, 1.5f, "410"),
            RoomData(4f, 3f, 1f, 1.5f, "409"),
            RoomData(5f, 3f, 1f, 1.5f, "408"),
            RoomData(6f, 3f, 1f, 1.5f, "407"),
            RoomData(7f, 3f, 1f, 1.5f, "406"),
            RoomData(8f, 3f, 1f, 1.5f, "405"),
            RoomData(9f, 3f, 1f, 1.5f, "404"),
            RoomData(10f, 3f, 1f, 1.5f, "403"),
            RoomData(11f, 3f, 2f, 1.5f, "402"),
            RoomData(13f, 3f, 1f, 1.5f, "401")
        )
    )

    val roomsByFloor3 = mapOf(
        1 to listOf(
            RoomData(1f, 1f, 1f, 2f, "Санитарно-бытовая комната"),
            RoomData(2f, 1f, 1f, 2f, "Санитарно-бытовая комната"),
            RoomData(3f, 1f, 4f,2f, "3"),
            RoomData(7f, 1f, 1.5f, 2f,  "4"),
            RoomData(1f, 4f, 2f, 3f, "Лаборатория"),
            RoomData(3f, 5f, 1f, 2f, "Лаборатория"),
            RoomData(9f, 2f, 2f, 3f, "6"),
            RoomData(8f, 5f, 3f, 2f, "5"),
            RoomData(6f, 5f, 2f, 2f, "Кабинет")
        ),
        2 to listOf(
            RoomData(1f, 1f, 2f, 3f, "Компьютерный класс"),
            RoomData(3f, 1f, 2f, 2f, "12"),
            RoomData(5f, 1f, 2f, 2f, "13"),
            RoomData(7f, 1f, 1.5f, 2f, "14"),
            RoomData(9f, 2f, 2f, 3f, "7"),
            RoomData(8f, 5f, 3f, 2f, "8"),
            RoomData(5f, 5f, 3f, 2f, "9"),
            RoomData(1f, 5f, 3f, 2f, "10"),
            RoomData(1f, 4f, 2f, 1f, "Кабинет")
        )
    )

    // Вычлинение номера корпуса (1-й символ преобразуется в число)  //Добавить проверку?
    var body = 0
    if (!floorPlan.building.isNullOrEmpty()) {
        body = floorPlan.building.substring(0, 1).toIntOrNull() ?: -1
    }
// Собираем ВСЕ комнаты выбранного корпуса (со всех этажей)
    val allRoomsInBuilding: List<RoomData> = when (body) {
        1 -> roomsByFloor1.values.flatten()
        2 -> roomsByFloor2.values.flatten()
        3 -> roomsByFloor3.values.flatten()
        else -> emptyList()
    }

// ✅ ПРОВЕРКА: есть ли искомая аудитория в корпусе?
    val roomExists = allRoomsInBuilding.any { it.number == floorPlan.roomNumber }

// Если корпус не распознан ИЛИ аудитория не найдена — не рисуем карту
    val shouldDraw = body in 1..3 && roomExists

    if (shouldDraw) {
        // Получаем список комнат ТОЛЬКО для текущего этажа
        val rooms = when (body) {
            1 -> roomsByFloor1[floorPlan.floor] ?: emptyList()
            2 -> roomsByFloor2[floorPlan.floor] ?: emptyList()
            3 -> roomsByFloor3[floorPlan.floor] ?: emptyList()
            else -> emptyList()
        }

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


/** Данные о комнате: позиция в ряду (1..13), позиция в столбце (1-5), ширина кабинета, высота кабинета, название кабинета */
private data class RoomData(
    val x: Float,
    val y: Float,
    val sizeX: Float,
    val sizeY: Float,
    val number: String
)