package com.example.bgtischedule.ui.mapper

import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.ScheduleUiModel.*

object LessonMapper {

    fun toUi(lesson: Lesson, indexInDay: Int = 0): LessonUi {
        return LessonUi(
            id = lessonUiId(lesson, indexInDay),
            lessonNumber = lesson.lessonNumber.toInt(),
            startTime = lesson.time.split("-").firstOrNull() ?: "",
            endTime = lesson.time.split("-").lastOrNull() ?: "",
            subject = lesson.subject,
            type = lesson.type,
            teacher = lesson.teacher,
            classroom = lesson.classroom,
            building = lesson.building,
            topic = lesson.topic,
            color = LessonColors.getColorForLesson(lesson.subject),
            floorPlan = FloorPlanUi(
                building = lesson.building,
                floor = extractFloor(lesson.classroom),
                roomNumber = lesson.classroom
            ),

        )
    }

    fun toDayGroups(lessons: List<Lesson>): List<DayGroupUi> {
        val deduped = lessons
            .groupBy { "${it.date}|${it.lessonNumber}" }
            .map { (_, dayLessons) ->
                dayLessons.maxByOrNull { it.id } ?: dayLessons.first()
            }

        return deduped
            .groupBy { it.date }
            .toSortedMap()
            .map { (date, dayLessons) ->
                DayGroupUi(
                    dayName = dayLessons.first().day,
                    date = formatDate(date),
                    lessons = dayLessons
                        .sortedBy { it.lessonNumber }
                        .mapIndexed { index, lesson -> toUi(lesson, index) }
                )
            }
    }

    private fun lessonUiId(lesson: Lesson, indexInDay: Int): String {
        if (lesson.id != 0L) return "lesson_${lesson.id}"
        return buildString {
            append(lesson.date)
            append('_')
            append(lesson.lessonNumber)
            append('_')
            append(lesson.subject)
            append('_')
            append(lesson.time)
            append('_')
            append(lesson.classroom)
            append('_')
            append(indexInDay)
        }
    }

    private fun extractFloor(classroom: String): Int {
        return classroom.firstOrNull()?.digitToIntOrNull() ?: 1
    }

    private fun formatDate(date: String): String {
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
