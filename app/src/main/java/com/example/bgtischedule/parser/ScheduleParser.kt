package com.example.bgtischedule.parser

import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.Schedule
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ScheduleParser {

    fun parse(html: String): Schedule? {
        return try {
            val doc = Jsoup.parse(html)

            // Имя и группа находятся в левой панели кабинета.
            val studentName = doc.selectFirst("div[style*=font-size:16px]")?.text()?.trim().orEmpty()
            val group = doc.selectFirst("div[style*=color:#e0e0e0]")?.text()?.trim().orEmpty()

            // Извлекаем диапазон недели
            val weekRange = doc.select("td")
                .firstOrNull { it.text().contains("Неделя с") }
                ?.text()
                ?.trim()
                .orEmpty()

            // Парсим таблицу с расписанием
            val lessons = parseLessons(doc)

            Schedule(studentName = studentName, group = group, weekRange = weekRange, lessons = lessons)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseLessons(doc: Document): List<Lesson> {
        val lessons = mutableListOf<Lesson>()
        val dayTables = doc.select("td.hdweek, td.hdweek-sl")
        for (dayHeader in dayTables) {
            val day = dayHeader.selectFirst("div.hdweekwk")?.text()?.trim().orEmpty()
            val date = dayHeader.ownText().trim()
            val table = dayHeader.closest("table") ?: continue

            table.select("tr").forEach { row ->
                val periodCell = row.selectFirst("td.period") ?: return@forEach
                val lessonNumber = periodCell.text().trim()
                if (!lessonNumber.matches(Regex("\\d+\\s+пара"))) return@forEach

                val lessonContainer = row.selectFirst("td:nth-child(2)") ?: return@forEach
                lessonContainer.select("div.lsnbox").forEach { box ->
                    parseLessonBox(day, date, lessonNumber, box.text())?.let { lessons.add(it) }
                }
            }
        }
        return lessons
    }

    private fun parseLessonBox(
        day: String,
        date: String,
        lessonNumber: String,
        info: String
    ): Lesson? {
        return try {
            val classroom = extractClassroom(info)
            val subject = extractSubject(info)
            val type = extractType(info)
            val teacher = extractTeacher(info)
            val topic = extractTopic(info)
            val time = getLessonTime(lessonNumber)

            Lesson(
                day = day,
                date = date,
                lessonNumber = lessonNumber,
                time = time,
                classroom = classroom,
                subject = subject,
                type = type,
                teacher = teacher,
                topic = topic
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractClassroom(info: String): String {
        val audRegex = "Ауд\\.\\s*([^\\n]+?)(?=\\s{2,}|Лекция|Практическое|Семинар|Тема занятия:|$)".toRegex()
        val aud = audRegex.find(info)?.groupValues?.get(1)?.trim()
        if (!aud.isNullOrBlank()) return aud

        // Иногда аудитория без "Ауд." (например "спортзал БКПТ")
        val firstLine = info.trim().lineSequence().firstOrNull()?.trim().orEmpty()
        return if (firstLine.isNotBlank()) firstLine else "Не указана"
    }

    private fun extractSubject(info: String): String {
        val regex = "(?:\\)\\s*|^)([^\\n]+?)(?=\\s+(?:Лекция|Практическое занятие|Практическое|Семинар)|Тема занятия:|$)".toRegex()
        val subject = regex.find(info)?.groupValues?.get(1)?.trim().orEmpty()
        return if (subject.isBlank() || subject.startsWith("Ауд.")) "Не указан" else subject
    }

    private fun extractType(info: String): String {
        return when {
            "Лекция" in info -> "Лекция"
            "Практическое" in info -> "Практическое занятие"
            "Семинар" in info -> "Семинар"
            else -> "Не указан"
        }
    }

    private fun extractTeacher(info: String): String {
        val regex = "([А-ЯЁ][а-яё]+\\s+[А-ЯЁ]\\.\\s*[А-ЯЁ]\\.)".toRegex()
        return regex.find(info)?.groupValues?.get(1)?.replace("\\s+".toRegex(), " ") ?: "Не указан"
    }

    private fun extractTopic(info: String): String {
        val regex = "Тема занятия:\\s*(.+)".toRegex()
        return regex.find(info)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun getLessonTime(lessonNumber: String): String {
        val times = mapOf(
            "1 пара" to "08:30-10:00",
            "2 пара" to "10:10-11:40",
            "3 пара" to "12:00-13:30",
            "4 пара" to "13:40-15:10",
            "5 пара" to "15:20-16:50",
            "6 пара" to "17:00-18:30",
            "7 пара" to "18:40-20:10",
            "8 пара" to "20:20-21:50"
        )
        return times[lessonNumber] ?: ""
    }
}