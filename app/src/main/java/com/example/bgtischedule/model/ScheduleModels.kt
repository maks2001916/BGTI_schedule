package com.example.bgtischedule.model

import com.example.bgtischedule.datebase.LessonEntity

//занятие
data class Lesson(
    /** ID записи в БД (0 — если с сервера без сохранения) */
    val id: Long = 0,
    /** Группа */
    val group: String,
    /** День недели */
    val day: String,
    /** Дата */
    val date: String,
    /** Номер пары */
    val lessonNumber: Byte,
    /** Время */
    val time: String,
    /** Аудитория */
    val classroom: String,
    /** Название предмета */
    val subject: String,
    /** Тип занятия */
    val type: String,
    /** Преподаватель */
    val teacher: String,
    /** Тема занятия */
    val topic: String,
    /** Примечание */
    val note: String,
    /** Оценка */
    val estimation: String,
    /** Примечание о времени начала занятия */
    val noteTime: String
) {

    fun toLessonDbEntity(): LessonEntity = LessonEntity(
        id = id,
        weekId = 0,
        group = group,
        dayOfWeek = day,
        date = date,
        lessonNumber = lessonNumber,
        time = time,
        classroom = classroom,
        subject = subject,
        type = type,
        teacher = teacher,
        topic = topic,
        note = note,
        estimation = estimation,
        noteTime = noteTime
    )
}

//график
data class Schedule(
    val studentFIO: StudentModel,
    val weekRange: String,
    val lessons: List<Lesson>
)