package com.example.bgtischedule.parser

import android.R
import androidx.compose.ui.Modifier
import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.model.StudentModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.coroutines.CoroutineContext

class ScheduleParser {

    fun parse(html: String?): Schedule? {
        return try {
            val doc = Jsoup.parse(html)

            // Имя и группа находящиеся в левой панели кабинета.
            val studentName = doc.selectFirst("div[style*=font-size:16px]")?.text()?.trim().orEmpty()
            val group = doc.selectFirst("div[style*=color:#e0e0e0]")?.text()?.trim().orEmpty()
            val parts = studentName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val student = StudentModel(parts[0], parts[1], parts[2], group = group)

            // Извлекаем диапазон недели
            val weekRange = doc.select("td")
                .firstOrNull {
                    it.attr("align") == "center" &&
                            it.text().contains("Неделя с") &&
                            it.attr("style").contains("font-size:18px")
                }
                ?.ownText()
                ?.trim()
                .orEmpty()

            // Парсим таблицу с расписанием
            val lessons = parseLessons(doc)

            Schedule(studentFIO = student, weekRange = weekRange, lessons = lessons)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Парсинг занятия
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
                    parseLessonBox(day, date, lessonNumber, box)?.let { lessons.add(it) }
                }
            }
        }
        return lessons
    }

    private fun parseLessonBox(
        day: String,
        date: String,
        lessonNumber: String,
        box: Element
    ): Lesson? {
        return try {

            var classroom = ""
            var subject = ""
            var type = ""
            var teacher = ""
            var topic = ""
            val time = getLessonTime(lessonNumber, false)
            var note = ""
            var estimation = ""
            var noteTime = ""


            val fullText = box.getTextWithStyle()

            for (styled in fullText) {
                when {
                    styled.style.contains("font-family:'RobotoMed', Tahoma, Arial") &&
                            styled.style.contains("font-size:18px") &&
                            isClassroom(styled.text) -> {
                        classroom = styled.text
                    }
                    styled.style.contains("margin-bottom:1px") -> {
                        subject = styled.text
                    }
                    styled.style.contains("text-shadow:none") &&
                            styled.style.contains("font-size:14px")
                            && styled.style.contains("color:#808080") -> {
                                type = styled.text
                            }
                    styled.style.contains("text-shadow:none") &&
                            styled.style.contains("font-style:italic") &&
                            styled.style.contains("font-size:14px") &&
                            styled.style.contains("margin-top:7px") -> {
                                teacher = styled.text
                            }
                    styled.style.contains("color:#909090") &&
                            styled.style.contains("text-shadow:none") &&
                            isTopic(styled.text) -> {
                                topic = styled.text
                            }
                    styled.style.contains("color:#909090") &&
                            styled.style.contains("text-shadow:none") &&
                            isNote(styled.text) -> {
                                note = styled.text
                            }
                    styled.style.contains("padding-top:7px") &&
                            styled.style.contains("color:#909090") &&
                            styled.style.contains("text-shadow:none") &&
                            isEstimation(styled.text) -> {
                                estimation = styled.text
                            }
                    isNoteTime(styled.text) -> {
                        noteTime = styled.text
                    }
                }
            }


            Lesson(
                day = day,
                date = date,
                lessonNumber = lessonNumber,
                time = time,
                classroom = classroom,
                subject = subject,
                type = type,
                teacher = teacher,
                topic = topic,
                note = note,
                estimation = estimation,
                noteTime = noteTime
            )
        } catch (e: Exception) {
            null
        }
    }

    // Парсинг кабинета
    private fun extractClassroom(info: String): String {
        val audRegex = "Ауд\\.\\s*([^\\n]+?)(?=\\s{2,}|Лекция|Практическое занятие|Лабораторная работа|Экзамен|Консультация|Зачёт|$)".toRegex()
        val aud = audRegex.find(info)?.groupValues?.get(1)?.trim()
        if (!aud.isNullOrBlank()) return aud

        // Иногда аудитория без "Ауд." (например "спортзал БКПТ")
        val firstLine = info.trim().lineSequence().firstOrNull()?.trim().orEmpty()
        return if (firstLine.isNotBlank()) firstLine else "Не указана"
    }

    private fun extractSubject(info: String): String {
        val regex = "(?:\\)\\s*|^)([^\\n]+?)(?=\\s+(?:|$))".toRegex()
        val subject = regex.find(info)?.groupValues?.get(1)?.trim().orEmpty()
        return if (subject.isBlank() || subject.startsWith("Ауд.")) "Не указан" else subject
    }

    private fun extractType(info: String): String {
        return when {
            "Лекция" in info -> "Лекция"
            "Практическое" in info -> "Практическое занятие"
            "Лабораторная" in info -> "Лабораторная работа"
            "Экзамен" in info -> "экзамен"
            "Консультация" in info -> "Консультация"
            "Зачёт" in info -> "Зачёт"

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

    private fun getNote(info: String): String {
        val regex = "Начало в\\s[0-2][0-4]\\.[0-6][0-9]".toRegex()
        return regex.find(info)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun getLessonTime(lessonNumber: String, shortTime: Boolean): String {
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
        val shortTimes = mapOf(
            "1 пара" to "8:30-9:40",
            "2 пара" to "9:50-11:00",
            "3 пара" to "11:10-12:20",
            "4 пара" to "12:30-13:40",
            "5 пара" to "13:50-15:00",
            "6 пара" to "15:10-16:20",
            "7 пара" to "16:30-17:40",
            "8 пара" to "17:50-19:00"
        )
        return if (shortTime) {
            shortTimes[lessonNumber] ?: ""
        } else times[lessonNumber] ?: ""
    }

    //методы для тестирования
    private fun Element.getTextWithStyle(): List<StyledText> {
        return this.select("*").mapNotNull { el ->
            val text = el.ownText().trim()
            val style = el.attr("style")
            if (text.isNotEmpty()) {
                StyledText(text, style)
            } else null
        }
    }

    data class StyledText(val text: String, val style: String)

    // 📍 Аудитория: Ауд. 304 (2 корпус)
    private fun isClassroom(text: String): Boolean {
        return text.startsWith("Ауд.") ||
                text.startsWith("спортзал") ||
                text.startsWith("чит.зал") ||
                Regex("""Ауд\.\s*\d{1,3}\s*\(""").containsMatchIn(text)
    }

    private fun isTopic(text: String): Boolean { return text.startsWith("Тема занятия: ") }
    private fun isNote(text: String): Boolean { return text.startsWith("Примечание: ") }
    private fun isEstimation(text: String): Boolean { return text.startsWith("Оценка: ") }
    private fun isNoteTime(text: String): Boolean { return text.startsWith("Начало в ") }
}