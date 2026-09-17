package com.example.bgtischedule.datebase

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 1,
    entities = [
        LessonEntity::class,
        ScheduleEntity::class,
        StudentEntity::class
    ]

)
abstract class AppDatabase: RoomDatabase() {
    abstract fun getScheduleDao(): ScheduleDao
}