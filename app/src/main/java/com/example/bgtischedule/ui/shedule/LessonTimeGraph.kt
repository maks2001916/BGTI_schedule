package com.example.bgtischedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * График времени пары с линией текущего времени
 *
 * @param startTime Начало пары ("08:30")
 * @param endTime Конец пары ("10:00")
 * @param currentTime Текущее время (если в диапазоне пары)
 * @param currentTimeColor Цвет линии текущего времени
 */


@Composable
fun LessonTimeGraph(
    startTime: String,
    endTime: String,
    currentTime: LocalTime? = null,
    currentTimeColor: Color? = null,
    modifier: Modifier = Modifier
        .fillMaxHeight()
        //.wrapContentWidth()
        //.width(20.dp)
) {
    val actualColor = currentTimeColor ?: MaterialTheme.colorScheme.primary
    val timeLineColor = MaterialTheme.colorScheme.outlineVariant
    val percentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textFontSize = MaterialTheme.typography.labelSmall.fontSize

    val textMeasurer = rememberTextMeasurer()
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    // Парсим время
    val start = LocalTime.parse(startTime, timeFormat)
    val end = LocalTime.parse(endTime, timeFormat)

    // Вычисляем позиции (в пределах 0-100%)
    val dayStart = LocalTime.of(8, 0)   // 08:00 - начало учебного дня
    val dayEnd = LocalTime.of(22, 0)    // 22:00 - конец учебного дня
    val dayDuration = Duration.between(dayStart, dayEnd).toMinutes()

    val startPercent = Duration.between(dayStart, start).toMinutes().toFloat() / dayDuration
    val endPercent = Duration.between(dayStart, end).toMinutes().toFloat() / dayDuration
    val currentPercent = currentTime?.let { Duration.between(dayStart, it).toMinutes().toFloat() / dayDuration }



        Canvas(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            val lineStartX = 0.dp.toPx()
            val lineEndX = size.width
            val lineTimeX = size.width / 2
            val timeStartX = 0.dp.toPx()
            val paddingY = 10.dp.toPx()


            val canvasWidth = size.width
            val canvasHeight = size.height
            val horizontalCenter = canvasWidth / 2

            /**
            drawLine(
                color = timeLineColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = timeLineColor,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = timeLineColor,
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = timeLineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2.dp.toPx()
            )
            */

            // Вертикальная ось времени
            drawLine(
                color = timeLineColor,
                start = Offset(lineTimeX, paddingY),
                end = Offset(lineTimeX, canvasHeight - paddingY),
                strokeWidth = 2.dp.toPx()
            )

            // Маркер начала пары
            val startY = 2f //startPercent * canvasWidth
            drawLine(
                color = percentColor,
                start = Offset(lineStartX, paddingY),
                end = Offset(lineEndX, paddingY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Подпись начала

            drawText(
                textMeasurer = textMeasurer,
                text = startTime,
                style = TextStyle(
                    color = textColor,
                    fontSize = textFontSize
                ),
                topLeft = Offset(timeStartX, startY - 5.dp.toPx())
            )

            // Маркер конца пары
            //val endY = endPercent * canvasHeight
            val endY = canvasHeight - 2
            drawLine(
                color = percentColor,
                start = Offset(lineStartX, endY - paddingY),
                end = Offset(lineEndX, endY - paddingY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Подпись конца
            drawText(
                textMeasurer = textMeasurer,
                text = endTime,
                style = TextStyle(
                    color = textColor,
                    fontSize = textFontSize
                ),
                topLeft = Offset(timeStartX, endY - 10.dp.toPx())
            )

            // Линия текущего времени (если в диапазоне)
            currentPercent?.let { percent ->
                if (percent in startPercent ..endPercent ) {
                    val currentY = percent * canvasHeight
                    drawLine(
                        color = actualColor,
                        start = Offset(lineStartX, currentY),
                        end = Offset(lineEndX, currentY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

}

@Preview(showBackground = true)
@Composable
fun LessonTimeGraphPreview() {
    LessonTimeGraph("8:30","10:00")
}