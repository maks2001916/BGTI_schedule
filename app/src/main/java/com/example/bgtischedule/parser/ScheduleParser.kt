package com.example.bgtischedule.parser

import com.example.bgtischedule.model.Lesson
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.model.StudentModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ScheduleParser {

    fun parse(html: String?): Schedule? {
        if (html.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.parse(html)

            // Имя и группа находящиеся в левой панели кабинета.
            val studentName = doc.selectFirst("div[style*=font-size:16px]")?.text()?.trim().orEmpty()
            val group = doc.selectFirst("div[style*=color:#e0e0e0]")?.text()?.trim().orEmpty()
            val parts = studentName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val student = StudentModel(
                name = parts.getOrElse(0) { "" },
                surname = parts.getOrElse(1) { "" },
                patronymic = parts.getOrElse(2) { "" },
                group = group
            )

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
        val dayTables = doc?.select("td.hdweek, td.hdweek-sl")
        if (dayTables != null) {
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
                    //кабинет
                    styled.style.contains("font-family:'RobotoMed', Tahoma, Arial") &&
                            styled.style.contains("font-size:18px") -> {
                        classroom = styled.text }
                    //предмет
                    styled.style.contains("margin-bottom:1px") -> {
                        subject = styled.text }
                    //тип занятия
                    styled.style.contains("text-shadow:none") &&
                            styled.style.contains("font-size:14px")
                            && styled.style.contains("color:#808080") -> {
                                type = styled.text }
                    //преподаватель
                    styled.style.contains("text-shadow:none") &&
                            styled.style.contains("font-style:italic") &&
                            styled.style.contains("font-size:14px") &&
                            styled.style.contains("margin-top:7px") -> {
                                teacher = styled.text }
                    //тема
                    styled.text.startsWith("Тема занятия:") -> {
                        topic = box.text().substringAfter("Тема занятия:", "") }
                    //примечание
                    styled.style.contains("color:#909090") &&
                            styled.style.contains("text-shadow:none") &&
                            styled.text.startsWith("Примечание:") -> {
                                note = styled.text }
                    //оценка
                    styled.style.contains("padding-top:7px") &&
                            styled.style.contains("color:#909090") &&
                            styled.style.contains("text-shadow:none") &&
                            styled.text.startsWith("Оценка:") -> {
                                estimation = styled.text }
                    //примечание о времени начала занятия
                    styled.text.startsWith("Начало в") -> {
                        noteTime = styled.text }
                }
            }


            val validDate = "2026-${getMonth(date)}-${getDate(date).toString().padStart(2, '0')}"

            Lesson(
                group = box.selectFirst("div[style*=color:#e0e0e0]")?.text()?.trim().orEmpty(),
                day = day,
                date = validDate,
                lessonNumber = getDate(lessonNumber).toByte(),
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

    private fun Element.getTextWithStyle(): List<StyledText> {
        return this.select("*").mapNotNull { el ->
            val text = el.ownText().trim()
            val style = el.attr("style")
            if (text.isNotEmpty()) {
                StyledText(text, style)
            } else null } }

    data class StyledText(val text: String, val style: String)
    private fun getDate(text: String): Int {
        val regex = Regex("\\d+")
        return regex.find(text)?.value?.toInt() ?: 0
    }

    private fun getMonth(text: String): String {
        val months = mapOf(
            "января" to "01", "февраля" to "02", "марта" to "03", "апреля" to "04",
            "мая" to "05", "июня" to "06", "июля" to "07", "августа" to "08",
            "сентября" to "09", "октября" to "10", "ноября" to "11", "декабря" to "12"
        )
        for ((name, num) in months) {
            if (text.contains(name, ignoreCase = true)) return num
        }
        return "01"
    }

    fun findWeekButtonDate(html: String?, titleContains: String): String? {
        if (html.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.parse(html)

            // Ищем все ячейки-кнопки и фильтруем по title — надёжнее, чем CSS-селектор с кириллицей
            val button = doc.select("td.hdweekbtn, td[onclick*=Default.aspx]")
                .firstOrNull { it.attr("title").contains(titleContains) }
                ?: return null

            val onclick = button.attr("onclick")
            Regex("""dt=(\d{2}\.\d{2}\.\d{4})""")
                .find(onclick)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}