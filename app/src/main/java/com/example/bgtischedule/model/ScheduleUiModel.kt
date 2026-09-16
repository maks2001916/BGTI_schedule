package com.example.bgtischedule.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.absoluteValue

class ScheduleUiModel {
    /** Предмет для отображения в расписании */
    data class LessonUi(
        val id: String,                          // Уникальный идентификатор
        val lessonNumber: Int,                   // Номер пары (1-8)
        val startTime: String,                   // "08:30"
        val endTime: String,                     // "10:00"
        val subject: String,                     // Название предмета
        val type: String,                        // Тип: "Лекция", "Практика"
        val teacher: String,                     // Преподаватель
        val classroom: String,                   // Аудитория
        val building: String,                    // Корпус
        val topic: String,                       // Тема занятия
        val color: Color,                        // Уникальный цвет карточки
        val floorPlan: FloorPlanUi               // План этажа
    )

    /** Декларативный план этажа для мини-отображения */
    data class FloorPlanUi(
        val building: String,                    // "2 корпус"
        val floor: Int,                          // 1-4
        val roomNumber: String,                  // "304"
        )

    /** Группа занятий по дню */
    data class DayGroupUi(
        val dayName: String,                     // "Понедельник"
        val date: String,                        // "13 апреля"
        val lessons: List<LessonUi>
    )

    /** Цветовая схема для предмета (генерируется по хэшу)*/
    object HashColors {
        private val palette = listOf(
            Color(0xFF90CAF9), Color(0xFFA5D6A7), Color(0xFFCE93D8),
            Color(0xFFFFAB91), Color(0xFF80DEEA), Color(0xFFE6EE9C),
            Color(0xFFB39DDB), Color(0xFFFFCC80)
        )

        /** Универсальный вход: любая строка-ключ (предмет, преподаватель…) */
        fun colorFor(key: String): Color {
            val t = fnv1a(key) / 0xFFFFFFFFL.toDouble()      // 0..1
            val pos = (t * (palette.size - 1)).coerceIn(0.0, (palette.size - 1).toDouble())
            val i = pos.toInt().coerceAtMost(palette.size - 2)
            return lerp(palette[i], palette[i + 1], (pos - i).toFloat())
        }

        // Соль в префиксе — чтобы один и тот же текст в разных сущностях не давал тот же цвет
        fun forSubject(subject: String): Color = colorFor("subj:$subject")
        fun forTeacher(teacher: String): Color = colorFor("teach:$teacher")

        /** FNV-1a 32: стабильнее и равномернее, чем String.hashCode */
        private fun fnv1a(s: String): Long {
            var h = 0x811C9DC5L
            for (c in s) {
                h = h xor c.code.toLong()
                h = (h * 0x01000193L) and 0xFFFFFFFFL
            }
            return h
        }
    }
}