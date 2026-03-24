package com.example.bgtischedule.datebaze

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.StudentModel

@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0 ,
    val weekId: Long,
    val studentGroup: String,

    val dayOfWeek: String,        //день недели
    val date: String,             //дата
    val lessonNumber: String,     //номер пары
    val time: String,             //вермя

    val subject: String,          //предмет
    val type: String,             //тип
    val topic: String,            //тема
    val teacher: String,          //преподаватель
    val classroom: String,         //аудитория

    val contentHash: String = ""
)

@Entity(tableName = "schedule_week")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentGroup: String,
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

