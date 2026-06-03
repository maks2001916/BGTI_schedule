package com.example.bgtischedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bgtischedule.model.ScheduleUiModel.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun LessonCard(
    lesson: LessonUi,
    currentTime: LocalTime,
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .wrapContentSize()
            //.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = lesson.color.copy(alpha = 0.15f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    lesson.color.copy(alpha = 0.3f),
                    lesson.color.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 🔢 Левая часть: номер пары (выделенный блок)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(12.dp)
                    .padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = lesson.color,
                    modifier = Modifier
                        .size(25.dp)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = "${lesson.lessonNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            /* // Правая часть: информация + график + план
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 // Заголовок: предмет + тип
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {*/
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.subject,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (lesson.type.isNotEmpty()) {
                            Text(
                                text = lesson.type,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Преподаватель
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lesson.teacher,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Тема занятия (если есть)
                        if (lesson.topic.isNotEmpty()) {
                            Text(
                                text = "${lesson.topic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                    }

            Column {
                // 🗺️ Мини-план этажа (справа)
                MiniFloorPlan(
                    floorPlan = lesson.floorPlan,
                    currentFloorColor = lesson.color
                )
                // кабинет
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = lesson.color.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = lesson.classroom,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = lesson.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
                /*}

            }*/
            // График времени
            /*LessonTimeGraph(
                startTime = lesson.startTime,
                endTime = lesson.endTime,
                currentTime = if (currentTime.toString().let { it >= lesson.startTime && it <= lesson.endTime })
                    currentTime else null,
                currentTimeColor = currentTimeColor,
                modifier = Modifier.wrapContentWidth().fillMaxHeight()
            )*/

        }
    }
}

@Preview(showBackground = true)
@Composable
fun LessonCardPreview() {
    LessonCard(
        lesson = LessonUi(
            "1",
            1,
            "9:00",
            "10:00",
            "ООП",
            "лекция",
            "Литвинова С.А.",
            "302",
            "тема",
            Color.Cyan,
            FloorPlanUi("2 корпус", 4, "302")
        ), LocalTime.now()
    )
}