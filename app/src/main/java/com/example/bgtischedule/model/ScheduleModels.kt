package com.example.bgtischedule.model

//занятие
data class Lesson(
    val day: String,            //день недели
    val date: String,           //дата
    val lessonNumber: String,   //номер пары
    val time: String,           //вермя
    val classroom: String,      //аудитория
    val subject: String,        //предмет
    val type: String,           //тип
    val teacher: String,        //преподаватель
    val topic: String           //тема
)

//график
data class Schedule(
    val studentFIO: StudentModel,
    val weekRange: String,
    val lessons: List<Lesson>
)