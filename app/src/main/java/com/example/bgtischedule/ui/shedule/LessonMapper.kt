package com.example.bgtischedule.ui.mapper

import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.ScheduleUiModel.*

object LessonMapper {

    fun toUi(lesson: Lesson): LessonUi {
        return LessonUi(
            id = "${lesson.date}_${lesson.lessonNumber}_${lesson.subject}",
            lessonNumber = lesson.lessonNumber.toInt(),
            startTime = lesson.time.split("-").firstOrNull() ?: "",
            endTime = lesson.time.split("-").lastOrNull() ?: "",
            subject = lesson.subject,
            type = lesson.type,
            teacher = lesson.teacher,
            classroom = lesson.classroom,
            topic = lesson.topic,
            color = LessonColors.getColorForLesson(lesson.subject),
            floorPlan = FloorPlanUi(
                building = "2 корпус",  // Заглушка — получить из данных
                floor = extractFloor(lesson.classroom),
                roomNumber = lesson.classroom
            )
        )
    }

    fun toDayGroups(lessons: List<Lesson>): List<DayGroupUi> {
        return lessons
            .groupBy { it.date }
            .map { (date, dayLessons) ->
                DayGroupUi(
                    dayName = dayLessons.first().day,
                    date = formatDate(date),
                    lessons = dayLessons.map { toUi(it) }
                )
            }
    }

    private fun extractFloor(classroom: String): Int {
        // Простая эвристика: первая цифра = этаж
        return classroom.firstOrNull()?.digitToIntOrNull() ?: 1
    }


    private fun formatDate(date: String): String {
        // "2026-04-13" → "13 апреля"
        return date.split("-")
            .takeIf { it.size == 3 }
            ?.let { "${it[2]} ${getMonthName(it[1].toInt())}" }
            ?: date
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "января"; 2 -> "февраля"; 3 -> "марта"; 4 -> "апреля"
            5 -> "мая"; 6 -> "июня"; 7 -> "июля"; 8 -> "августа"
            9 -> "сентября"; 10 -> "октября"; 11 -> "ноября"; 12 -> "декабря"
            else -> ""
        }
    }
}