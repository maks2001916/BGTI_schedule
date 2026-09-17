package com.example.bgtischedule.service

import android.util.Log
import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.data.model.SyncResult
import com.example.bgtischedule.datebase.ScheduleRepository
import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.model.StudentModel
import com.example.bgtischedule.parser.ScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val TAG = "Request"

class Request(
    private val repository: ScheduleRepository,
    private val parser: ScheduleParser,
    private val api: UniversityApi
) {

    /** Основная функция синхронизации расписания */
    suspend fun syncSchedule(
        group: String,
        login: String,
        password: String,
        weekOffset: Int
    ): SyncResult = withContext(Dispatchers.IO) {
        if (weekOffset > 0)
            syncNextWeek(group, login, password)
        else if (weekOffset < 0)
            syncPreviousWeek(group, login, password)
        else
            syncThisWeek(group, login, password)
    }

    suspend fun loadCachedWeek(group: String, weekStart: LocalDate): SyncResult =
        withContext(Dispatchers.IO) {
            try {
                val week = repository.getWeekForData(group, weekStart.toString())
                    ?: return@withContext SyncResult.Error("Нет сохранённой недели")
                val lessons = repository
                    .getLessonsForWeek(group, week.weekStart, week.weekEnd)
                    .map { it.toLesson() }
                if (lessons.isEmpty()) return@withContext SyncResult.Error("Нет сохранённого расписания")

                SyncResult.Cached(Schedule(
                    StudentModel("", "", "", group),
                    "${week.weekStart} — ${week.weekEnd}",
                    lessons
                ))
            } catch (e: Exception) {
                SyncResult.Error("Ошибка кэша: ${e.message}", e)
            }
        }

    suspend fun refreshWeek(
        group: String,
        login: String,
        password: String,
        weekOffset: Int
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            if (!api.login(login, password))
                return@withContext SyncResult.Error("Не удалось авторизоваться")

            // Текущая страница сайта
            var html = api.getSchedulePage()
                ?: return@withContext SyncResult.Error("Нет ответа от сервера")

            // Переходим к запрошенной неделе через КНОПКИ сайта:
            // дата из кнопки → ЗАГРУЗКА страницы по дате → (при offset>1) повторяем
            if (weekOffset != 0) {
                val title = if (weekOffset > 0) "Следующая неделя" else "Предыдущая неделя"
                repeat(kotlin.math.abs(weekOffset)) {
                    val date = parser.findWeekButtonDate(html, title)
                        ?: return@withContext SyncResult.Error("Неделя недоступна на сайте")
                    html = api.getSchedulePage(date)   // ✅ ВОТ ЭТОТ ШАГ БЫЛ ПРОПУЩЕН
                        ?: return@withContext SyncResult.Error("Не удалось загрузить страницу недели")
                }
            }

            val newSchedule = parser.parse(html)
                ?: return@withContext SyncResult.Error("Ошибка парсинга")

            val resolvedGroup = group.ifBlank { newSchedule.studentFIO.group }

            // Сравниваем с БД; изменения сохраняем
            val changes = detectChanges(resolvedGroup, newSchedule)
            if (changes.isNotEmpty()) {
                saveNewRecords(resolvedGroup, newSchedule, changes) // или saveScheduleWithChanges
            }
            SyncResult.Success(newSchedule, changes)
        } catch (e: Exception) {
            Log.e(TAG, "refreshWeek error", e)
            SyncResult.Error(e.message ?: "Ошибка загрузки изменений")
        }
    }

    suspend fun syncThisWeek(
        group: String,
        login: String,
        password: String
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val isLoggedIn = api.login(login, password)
            if (!isLoggedIn) {
                Log.e(TAG, "Ошибка авторизации")
                return@withContext loadFromCache(group)
            }

            val html = api.getSchedulePage()
            if (html == null) {
                Log.e(TAG, "Не удалось загрузить страницу расписания")
                return@withContext loadFromCache(group)
            }

            val newSchedule = parser.parse(html)
            if (newSchedule == null) {
                Log.e(TAG, "Ошибка парсинга")
                return@withContext loadFromCache(group)
            }

            val resolvedGroup = group.ifBlank { newSchedule.studentFIO.group }
            Log.i(TAG, "syncSchedule: group=$resolvedGroup, lessons=${newSchedule.lessons.size}")

            val changes = detectChanges(resolvedGroup, newSchedule)

            if (changes.isNotEmpty()) {
                saveNewRecords(resolvedGroup, newSchedule, changes)
                SyncResult.Success(newSchedule, changes)
            } else {
                val cached = loadFromCache(resolvedGroup)
                if (cached is SyncResult.Cached) {
                    SyncResult.Success(cached.schedule, source = SyncResult.Source.CACHE)
                } else {
                    SyncResult.Success(newSchedule)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            loadFromCache(group)
        }
    }


    suspend fun syncNextWeek(
        group: String,
        login: String,
        password: String
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val isLoggedIn = api.login(login, password)
            if (!isLoggedIn) {
                Log.e(TAG, "Ошибка авторизации")
                return@withContext loadFromCache(group)
            }

            val html = api.getSchedulePage()
            if (html == null) {
                Log.e(TAG, "Не удалось загрузить страницу расписания")
                return@withContext loadFromCache(group)
            }

            val newSchedule = parser.parse(html)
            if (newSchedule == null) {
                Log.e(TAG, "Ошибка парсинга")
                return@withContext loadFromCache(group)
            }

            val htmlFirst = parser.findWeekButtonDate(html, "Следующая неделя")
            if (htmlFirst == null) {
                Log.e(TAG, "Не удалось загрузить страницу расписания")
                return@withContext loadFromCache(group)
            }

            val newScheduleHtmlFirst = parser.parse(htmlFirst)
            if (newScheduleHtmlFirst == null) {
                Log.e(TAG, "Ошибка парсинга")
                return@withContext loadFromCache(group)
            }

            val resolvedGroup = group.ifBlank { newSchedule.studentFIO.group }
            Log.i(TAG, "syncSchedule: group=$resolvedGroup, lessons=${newSchedule.lessons.size}")

            val changesHtmlFirst = detectChanges(resolvedGroup, newScheduleHtmlFirst)

            if (changesHtmlFirst.isNotEmpty()) {
                saveNewRecords(resolvedGroup, newScheduleHtmlFirst, changesHtmlFirst)
                SyncResult.Success(newScheduleHtmlFirst, changesHtmlFirst)
            } else {
                val cached = loadFromCache(resolvedGroup)
                if (cached is SyncResult.Cached) {
                    SyncResult.Success(cached.schedule, source = SyncResult.Source.CACHE)
                } else {
                    SyncResult.Success(newScheduleHtmlFirst)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            loadFromCache(group)
        }
    }

    suspend fun syncPreviousWeek(
        group: String,
        login: String,
        password: String
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val isLoggedIn = api.login(login, password)
            if (!isLoggedIn) {
                Log.e(TAG, "Ошибка авторизации")
                return@withContext loadFromCache(group)
            }

            val html = api.getSchedulePage()
            if (html == null) {
                Log.e(TAG, "Не удалось загрузить страницу расписания")
                return@withContext loadFromCache(group)
            }

            val newSchedule = parser.parse(html)
            if (newSchedule == null) {
                Log.e(TAG, "Ошибка парсинга")
                return@withContext loadFromCache(group)
            }

            val htmlFirst = parser.findWeekButtonDate(html, "Предыдущая неделя")
            if (htmlFirst == null) {
                Log.e(TAG, "Не удалось загрузить страницу расписания")
                return@withContext loadFromCache(group)
            }

            val newScheduleHtmlFirst = parser.parse(htmlFirst)
            if (newScheduleHtmlFirst == null) {
                Log.e(TAG, "Ошибка парсинга")
                return@withContext loadFromCache(group)
            }

            val resolvedGroup = group.ifBlank { newSchedule.studentFIO.group }
            Log.i(TAG, "syncSchedule: group=$resolvedGroup, lessons=${newSchedule.lessons.size}")

            val changesHtmlFirst = detectChanges(resolvedGroup, newScheduleHtmlFirst)

            if (changesHtmlFirst.isNotEmpty()) {
                saveNewRecords(resolvedGroup, newScheduleHtmlFirst, changesHtmlFirst)
                SyncResult.Success(newScheduleHtmlFirst, changesHtmlFirst)
            } else {
                val cached = loadFromCache(resolvedGroup)
                if (cached is SyncResult.Cached) {
                    SyncResult.Success(cached.schedule, source = SyncResult.Source.CACHE)
                } else {
                    SyncResult.Success(newScheduleHtmlFirst)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            loadFromCache(group)
        }
    }

    /** Загрузка расписания только из локальной БД (без сети) */
    suspend fun loadCachedSchedule(group: String): SyncResult = withContext(Dispatchers.IO) {
        loadFromCache(group)
    }

    private suspend fun loadFromCache(group: String, ): SyncResult {
        return try {
            val today = LocalDate.now().toString()
            val week = repository.getWeekForData(group, today)

            if (week != null) {
                val lessons = repository.getLessonsForWeek(group, week.weekStart, week.weekEnd)
                    .map { it.toLesson() }
                SyncResult.Cached(
                    Schedule(
                        studentFIO = StudentModel("", "", "", group),
                        weekRange = "${week.weekStart} — ${week.weekEnd}",
                        lessons = lessons
                    )
                )
            } else {
                SyncResult.Error("Нет сохранённого расписания")
            }
        } catch (e: Exception) {
            SyncResult.Error("Ошибка загрузки из кэша", e)
        }
    }

    private suspend fun detectChanges(
        group: String,
        newSchedule: Schedule
    ): List<SyncResult.Change> {
        val changes = mutableListOf<SyncResult.Change>()
        val (weekStart, weekEnd) = parseWeekRange(newSchedule.weekRange)
        if (weekStart.isBlank() || weekEnd.isBlank()) {
            return newSchedule.lessons.map { lesson ->
                SyncResult.Change(
                    type = SyncResult.ChangeType.ADDED,
                    lessonNumber = lesson.lessonNumber,
                    date = lesson.date,
                    subject = lesson.subject,
                    newValues = mapOf(
                        "time" to lesson.time,
                        "classroom" to lesson.classroom,
                        "teacher" to lesson.teacher
                    )
                )
            }
        }

        val oldLessons = repository.getLessonsByWeekRange(group, weekStart, weekEnd)
        val oldMap = oldLessons.associateBy { "${it.date}|${it.lessonNumber}" }
        val newMap = newSchedule.lessons.associateBy { "${it.date}|${it.lessonNumber}" }

        for (newLesson in newSchedule.lessons) {
            val key = "${newLesson.date}|${newLesson.lessonNumber}"
            val oldLesson = oldMap[key]

            if (oldLesson == null) {
                changes.add(SyncResult.Change(
                    type = SyncResult.ChangeType.ADDED,
                    lessonNumber = newLesson.lessonNumber,
                    date = newLesson.date,
                    subject = newLesson.subject,
                    newValues = mapOf(
                        "time" to newLesson.time,
                        "classroom" to newLesson.classroom,
                        "teacher" to newLesson.teacher
                    )
                ))
            } else if (oldLesson.contentHash != newLesson.toLessonDbEntity().contentHash) {
                val fieldChanges = detectFieldChanges(oldLesson.toLesson(), newLesson)
                if (fieldChanges.first.isNotEmpty()) {
                    changes.add(SyncResult.Change(
                        type = SyncResult.ChangeType.MODIFIED,
                        lessonNumber = newLesson.lessonNumber,
                        date = newLesson.date,
                        subject = newLesson.subject,
                        oldValues = fieldChanges.first,
                        newValues = fieldChanges.second
                    ))
                }
            }
        }

        for (oldLesson in oldLessons) {
            val key = "${oldLesson.date}|${oldLesson.lessonNumber}"
            if (newMap[key] == null) {
                changes.add(SyncResult.Change(
                    type = SyncResult.ChangeType.REMOVED,
                    lessonNumber = oldLesson.lessonNumber,
                    date = oldLesson.date,
                    subject = oldLesson.subject,
                    oldValues = mapOf(
                        "time" to oldLesson.time,
                        "classroom" to oldLesson.classroom
                    )
                ))
            }
        }

        return changes
    }

    private fun detectFieldChanges(
        oldLesson: Lesson,
        newLesson: Lesson
    ): Pair<Map<String, String?>, Map<String, String?>> {
        val oldValues = mutableMapOf<String, String?>()
        val newValues = mutableMapOf<String, String?>()

        val fields = listOf(
            Triple("time", oldLesson.time, newLesson.time),
            Triple("classroom", oldLesson.classroom, newLesson.classroom),
            Triple("teacher", oldLesson.teacher, newLesson.teacher),
            Triple("topic", oldLesson.topic, newLesson.topic),
            Triple("type", oldLesson.type, newLesson.type)
        )
        for ((field, oldValue, newValue) in fields) {
            if (oldValue != newValue) {
                oldValues[field] = oldValue
                newValues[field] = newValue
            }
        }

        return oldValues to newValues
    }

    private suspend fun saveNewRecords(
        group: String,
        schedule: Schedule,
        changes: List<SyncResult.Change>
    ): List<Long> {
        val (weekStart, weekEnd) = parseWeekRange(schedule.weekRange)
        val existingWeek = if (weekStart.isNotBlank()) {
            repository.getWeekForData(group, weekStart)
        } else null

        val lessonEntities = schedule.lessons.map {
            it.toLessonDbEntity().copy(group = group, contentHash = generateContentHash(it))
        }

        if (existingWeek == null) {
            repository.setWeek(
                group = group,
                weekStart = weekStart,
                weekEnd = weekEnd,
                newLessons = lessonEntities
            )
            return emptyList()
        }

        val insertedIds = mutableListOf<Long>()
        val weekId = existingWeek.id

        for (change in changes) {
            val newLesson = schedule.lessons.find {
                it.date == change.date && it.lessonNumber == change.lessonNumber
            } ?: continue

            val lessonEntity = newLesson.toLessonDbEntity().copy(
                group = group,
                weekId = weekId,
                contentHash = generateContentHash(newLesson)
            )

            val id = repository.setLesson(lessonEntity)
            insertedIds.add(id)
            Log.i(TAG, "Inserted lesson: id=$id, date=${change.date}, subject=${change.subject}")
        }

        return insertedIds
    }

    private fun parseWeekRange(weekRange: String): Pair<String, String> {
        val isoRegex = """(\d{4}-\d{2}-\d{2})""".toRegex()
        val isoDates = isoRegex.findAll(weekRange).map { it.value }.toList()
        if (isoDates.size >= 2) return isoDates[0] to isoDates[1]

        val dotRegex = """(\d{2})\.(\d{2})\.(\d{4})""".toRegex()
        val parsed = dotRegex.findAll(weekRange).map { match ->
            "${match.groupValues[3]}-${match.groupValues[2]}-${match.groupValues[1]}"
        }.toList()
        return if (parsed.size >= 2) parsed[0] to parsed[1] else "" to ""
    }

    private fun generateContentHash(lesson: Lesson): String {
        return "${lesson.time}|${lesson.classroom}|${lesson.teacher}|${lesson.topic}|${lesson.type}"
            .hashCode()
            .toString()
    }
}
