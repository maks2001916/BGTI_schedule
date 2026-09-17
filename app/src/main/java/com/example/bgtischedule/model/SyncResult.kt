package com.example.bgtischedule.data.model

import com.example.bgtischedule.model.Schedule

/**
 * Результат синхронизации расписания
 */
sealed class SyncResult {
    /** Успешная загрузка с сервера */
    data class Success(
        val schedule: Schedule,
        val changes: List<Change> = emptyList(),
        val source: Source = Source.SERVER
    ) : SyncResult()

    /** Загрузка из кэша */
    data class Cached(
        val schedule: Schedule,
        val source: Source = Source.CACHE
    ) : SyncResult()

    /** Ошибка */
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()

    enum class Source { SERVER, CACHE }

    /** Тип изменения в расписании */
    data class Change(
        val type: ChangeType,
        val lessonNumber: Byte,
        val date: String,
        val subject: String,
        val oldValues: Map<String, String?> = emptyMap(),
        val newValues: Map<String, String?> = emptyMap()
    )

    enum class ChangeType { ADDED, REMOVED, MODIFIED }
}