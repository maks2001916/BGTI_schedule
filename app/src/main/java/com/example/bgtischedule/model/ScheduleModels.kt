package com.example.bgtischedule.model

data class Lesson(
    val day: String,
    val date: String,
    val lessonNumber: String,
    val time: String,
    val classroom: String,
    val subject: String,
    val type: String,
    val teacher: String,
    val topic: String
)

data class Schedule(
    val studentFIO: StudentModel,
    val group: String,
    val weekRange: String,
    val lessons: List<Lesson>
)