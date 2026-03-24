package com.example.bgtischedule.datebaze

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.StudentModel

@Entity(tableName = "lesson")
data class LessonDbEntity(
    @ColumnInfo(name = "day") val day: String,                      //день недели
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String,                    //дата
    @ColumnInfo(name = "lessonNumber") val lessonNumber: String,    //номер пары
    @ColumnInfo(name = "time") val time: String,                    //вермя
    @ColumnInfo(name = "classroom") val classroom: String,          //аудитория
    @ColumnInfo(name = "subject") val subject: String,              //предмет
    @ColumnInfo(name = "type") val type: String,                    //тип
    @ColumnInfo(name = "teacher") val teacher: String,              //преподаватель
    @ColumnInfo(name = "topic") val topic: String                   //тема
)

@Entity(tableName = "schedule")
data class ScheduleDbEntity(
    @ColumnInfo(name = "student") val student: StudentModel,
    @PrimaryKey
    @ColumnInfo(name = "weekRange") val weekRange: String,
    @ColumnInfo(name = "group") val group: String,
    @ColumnInfo(name = "lessons") val lessons: List<Lesson>
)

@Entity
data class StudentDbEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "surname") val surname: String,
    @ColumnInfo(name = "patronymic") val patronymic: String,
    @ColumnInfo(name = "group") val group: String
)

