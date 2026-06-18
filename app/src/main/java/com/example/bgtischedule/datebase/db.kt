package com.example.bgtischedule.datebase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.Schedule

@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("student_group", "date"),
        Index("weekId")
    ]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekId: Long,
    @ColumnInfo(name = "student_group")
    val group: String,

    /** день недели */
    val dayOfWeek: String,
    /** дата */
    val date: String,
    /** номер пары */
    val lessonNumber: Byte,
    /** вермя */
    val time: String,
    /** аудитория */
    val classroom: String,
    /** предмет */
    val subject: String,
    /** тип */
    val type: String,
    /** преподаватель */
    val teacher: String,
    /** тема */
    val topic: String,
    /** примечание */
    val note: String,
    /** оценка */
    val estimation: String,
    /** примечание о времени начала занятия */
    val noteTime: String,

    val contentHash: String = ""
)  {
    fun toLesson(): Lesson = Lesson(
        id = id,
        group = group,
        /** день недели */
        day = dayOfWeek,
        /** дата */
        date = date,
        /** номер пары */
        lessonNumber = lessonNumber,
        /** вермя */
        time = time,
        /** аудитория */
        classroom = classroom,
        /** предмет */
        subject = subject,
        /** тип */
        type = type,
        /** преподаватель */
        teacher = teacher,
        /** тема */
        topic = topic,
        /** примечание */
        note = note,
        /** оценка */
        estimation = estimation,
        /** примечание о времени начала занятия */
        noteTime = noteTime
    )
}

@Entity(tableName = "schedule_week")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_group")
    val group: String,
    val weekStart: String,
    val weekEnd: String,
    val lastUpdated: Long
)

@Entity(tableName = "student")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val name: String,
    val surname: String,
    val patronymic: String,
    val group: String,
    val login: String = ""
)

