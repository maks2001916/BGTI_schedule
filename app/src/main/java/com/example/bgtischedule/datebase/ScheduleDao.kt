package com.example.bgtischedule.datebase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    //
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setWeek(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLessons(lessons: List<LessonEntity>): List<Long>


    /** Получить неделю, содержащую указанную дату */
    @Query("""
        SELECT * FROM schedule_week 
        WHERE student_group = :group 
        AND :dateIso BETWEEN weekStart AND weekEnd 
        LIMIT 1
    """)
    suspend fun getWeekForDate(group: String, dateIso: String): ScheduleEntity?
    
    @Delete
    suspend fun deleteWeek(schedule: ScheduleEntity): Int

    // Получить расписание на неделю
    @Query("""
        SELECT * FROM lesson 
        WHERE student_group = :group AND weekId = :weekId 
        ORDER BY date, lessonNumber
    """)
    fun getLessonsByWeekFlow(group: String, weekId: Long): Flow<List<LessonEntity>>

    @Query("""
        SELECT * FROM lesson 
        WHERE student_group = :group 
        AND date BETWEEN :weekStart AND :weekEnd 
        ORDER BY date, lessonNumber
    """)
    fun getLessonsByWeekRangeFlow(
        group: String,
        weekStart: String,
        weekEnd: String
    ): Flow<List<LessonEntity>>

    // Получить расписание на конкретный день
    @Query("""
        SELECT * FROM lesson 
        WHERE student_group = :group AND date = :dateIso 
        ORDER BY lessonNumber
    """)
    fun getLessonsByDateFlow(group: String, dateIso: String): Flow<List<LessonEntity>>

    @Query("""
        SELECT * FROM lesson 
        WHERE student_group = :group AND date = :dateIso 
        ORDER BY lessonNumber
    """)
    suspend fun getLessonsByDate(group: String, dateIso: String): List<LessonEntity>

    // Удалить расписание на неделю
    @Query("DELETE FROM lesson WHERE weekId = :weekId")
    suspend fun deleteLessonsByWeekId(weekId: Long): Int

    @Query("""
        DELETE FROM lesson 
        WHERE student_group = :group 
        AND date BETWEEN :weekStart AND :weekEnd
    """)
    suspend fun deleteLessonsByWeekRange(group: String, weekStart: String, weekEnd: String): Int

    // Удалить расписание на конкретный день
    @Query("""
        DELETE FROM lesson 
        WHERE student_group = :group AND date = :dateIso
    """)
    suspend fun deleteLessonsByDate(group: String, dateIso: String): Int


}