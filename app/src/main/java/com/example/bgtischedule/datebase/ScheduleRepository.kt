package com.example.bgtischedule.datebase

import com.example.bgtischedule.data.auth.AuthStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val authManager: AuthStateManager
) {

    /** Вспомогательная функция: выбросить исключение, если не авторизован */
    private fun requireAuth() {
        if (!authManager.authState.value.isAuthenticated) {
            throw SecurityException("Требуется авторизация для доступа к расписанию")
        }
    }

    /** Получить расписание на день (с проверкой авторизации) */
    fun getLessonsByDateFlow(dateIso: String): Flow<List<LessonEntity>> {
        requireAuth()
        val group = authManager.getCurrentGroup()
            ?: throw IllegalStateException("Группа не определена")
        return scheduleDao.getLessonsByDateFlow(group, dateIso)
    }

    /** Получить расписание за неделю (для синхронизации, без проверки auth) */
    suspend fun getLessonsByWeekRange(group: String, weekStart: String, weekEnd: String): List<LessonEntity> {
        return withContext(Dispatchers.IO) {
            scheduleDao.getLessonsByWeekRangeFlow(group, weekStart, weekEnd).first()
        }
    }

    /** Получить расписание за неделю из кэша */
    suspend fun getLessonsForWeek(group: String, weekStart: String, weekEnd: String): List<LessonEntity> {
        return withContext(Dispatchers.IO) {
            scheduleDao.getLessonsByWeekRangeFlow(group, weekStart, weekEnd).first()
        }
    }


    /** Сохранить неделю с парами (с проверкой авторизации) */
    suspend fun setWeek(
        weekStart: String,
        weekEnd: String,
        lessons: List<LessonEntity>
    ) {
        requireAuth()  // ✅ Проверка перед записью
        val group = authManager.getCurrentGroup()
            ?: throw IllegalStateException("Группа не определена")

        withContext(Dispatchers.IO) {
            if (lessons.isNotEmpty()) {
                val schedule = ScheduleEntity(
                    group = group,
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    lastUpdated = System.currentTimeMillis()
                )
                val weekId = scheduleDao.setWeek(schedule)
                val lessonsWithWeek = lessons.map { it.copy(weekId = weekId) }
                scheduleDao.setLessons(lessonsWithWeek)
            }
        }
    }


    /**
     * Добавить одну пару в расписание
     * @return ID сохранённой записи
     */
    suspend fun setLesson(lesson: LessonEntity): Long {
        return withContext(Dispatchers.IO) {
            scheduleDao.setLessons(listOf(lesson)).firstOrNull() ?: 0
        }
    }

    /**
     * Добавить несколько пар (пакетное сохранение)
     * @return список ID сохранённых записей
     */
    suspend fun setLessons(lessons: List<LessonEntity>): List<Long> {
        return withContext(Dispatchers.IO) {
            scheduleDao.setLessons(lessons)
        }
    }

    // ========================================================================
    // ===  УДАЛИТЬ пару ===
    // ========================================================================

    /**
     * Удалить пару по ID
     */
    suspend fun deleteLessonById(lessonId: Long) {
        withContext(Dispatchers.IO) {
            // Примечание: добавьте в DAO метод deleteLessonById если нужно
        }
    }

    /**
     * Удалить пары на конкретную дату
     */
    suspend fun deleteLessonsByDate(group: String, dateIso: String) {
        withContext(Dispatchers.IO) {
            scheduleDao.deleteLessonsByDate(group, dateIso)
        }
    }

    suspend fun deleteLessonsByWeekRange(group: String, weekStart: String, weekEnd: String) {
        withContext(Dispatchers.IO) {
            scheduleDao.deleteLessonsByWeekRange(group, weekStart, weekEnd)
        }
    }

    // ========================================================================
    // === ДОБАВИТЬ НЕДЕЛЮ ===
    // ========================================================================

    /**
     * Сохранить неделю и привязанные к ней пары
     * @return ID сохранённой недели
     */
    suspend fun setWeekWithLessons(
        schedule: ScheduleEntity,
        lessons: List<LessonEntity>
    ): Long {
        return withContext(Dispatchers.IO) {
            // 1. Сохраняем неделю, получаем её ID
            val weekId = scheduleDao.setWeek(schedule)

            // 2. Привязываем пары к неделе и сохраняем
            val lessonsWithWeek = lessons.map { it.copy(weekId = weekId) }
            scheduleDao.setLessons(lessonsWithWeek)

            weekId
        }
    }

    /** Сохранить расписание на неделю */
    suspend fun setWeek(
        group: String,
        weekStart: String,
        weekEnd: String,
        newLessons: List<LessonEntity>
    ) {
        withContext(Dispatchers.IO) {
            if (newLessons.isNotEmpty()) {
                val schedule = ScheduleEntity(
                    group = group,
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    lastUpdated = System.currentTimeMillis()
                )
                val weekId = scheduleDao.setWeek(schedule)
                val lessonsWithWeek = newLessons.map { it.copy(weekId = weekId) }
                scheduleDao.setLessons(lessonsWithWeek)
            }
        }
    }

    /** Проверить, есть ли данные за неделю */
    suspend fun hasWeekData(group: String, date: String): Boolean {
        return withContext(Dispatchers.IO) {
            val week = scheduleDao.getWeekForDate(group, date)
            week != null
        }
    }

    /** Проверить, есть ли данные за неделю */
    suspend fun getWeekForData(group: String, date: String): ScheduleEntity? {
        return scheduleDao.getWeekForDate(group, date)
    }


    /** Получить дату последнего обновления недели */
    suspend fun getWeekLastUpdate(group: String, date: String): Long? {
        return withContext(Dispatchers.IO) {
            scheduleDao.getWeekForDate(group, date)?.lastUpdated
        }
    }

    /** Очистить все данные студента (при выходе) */
    suspend fun clearAllData(group: String) {
        withContext(Dispatchers.IO) {
            scheduleDao.deleteLessonsByWeekRange(group, "1900-01-01", "2100-12-31")
        }
    }
}