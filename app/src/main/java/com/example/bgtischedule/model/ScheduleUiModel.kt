package com.example.bgtischedule.model

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

class ScheduleUiModel {
    /**
     * Предмет для отображения в расписании
     */
    data class LessonUi(
        val id: String,                          // Уникальный идентификатор
        val lessonNumber: Int,                   // Номер пары (1-8)
        val startTime: String,                   // "08:30"
        val endTime: String,                     // "10:00"
        val subject: String,                     // Название предмета
        val type: String,                        // Тип: "Лекция", "Практика"
        val teacher: String,                     // Преподаватель
        val classroom: String,                   // Аудитория
        val topic: String,                       // Тема занятия
        val color: Color,                        // Уникальный цвет карточки
        val floorPlan: FloorPlanUi               // План этажа
    )

    /**
     * Декларативный план этажа для мини-отображения
     */
    data class FloorPlanUi(
        val building: String,                    // "2 корпус"
        val floor: Int,                          // 1-4
        val roomNumber: String,                  // "304"
        //val roomPosition: RoomPosition           // Позиция комнаты на плане
    )

    /**
     * Группа занятий по дню
     */
    data class DayGroupUi(
        val dayName: String,                     // "Понедельник"
        val date: String,                        // "13 апреля"
        val lessons: List<LessonUi>
    )

    /**
     * Цветовая схема для предмета (генерируется по хэшу)
     */
    object LessonColors {
        private val palette = listOf(
            Color(0xFF90CAF9), Color(0xFFA5D6A7), Color(0xFFCE93D8),
            Color(0xFFFFAB91), Color(0xFF80DEEA), Color(0xFFE6EE9C),
            Color(0xFFB39DDB), Color(0xFFFFCC80)
        )

        fun getColorForLesson(id: String): Color {
            val index = id.hashCode().absoluteValue % palette.size
            return palette[index]
        }
    }
}