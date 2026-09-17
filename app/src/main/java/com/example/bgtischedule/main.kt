package com.example.bgtischedule

import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.parser.ScheduleParser
import kotlinx.coroutines.runBlocking
import java.io.File

private val logUrl: String = "https://bgti.ru//Enter/Signin.aspx"
private val sheduleUrl: String = "https://lk.bgti.ru/Default.aspx"



fun main() = runBlocking {
    println("=== Парсер расписания БГТИ ===\n")

    val api = UniversityApi(logUrl) // Укажите реальный URL
    val parser = ScheduleParser()
    println("Выбор режима")
    println("1: web")
    println("2: local")
    print("ввод: ")
    val input = readLine()
    if (input?.toInt() == 1) {
        chekSheduleWeb(api, parser)
    } else if (input?.toInt() == 2) {
        chekScheduleLocal()
    }

}

fun chekSheduleWeb(api: UniversityApi, parser: ScheduleParser) = runBlocking {
    // Ввод данных
    print("Логин: ")
    val login = readLine() ?: return@runBlocking

    print("Пароль: ")
    val password = readLine() ?: return@runBlocking

    println("\n🔐 Авторизация...")
    val isLoggedIn = api.login(login, password)

    if (!isLoggedIn) {
        println("❌ Ошибка авторизации!")
        return@runBlocking
    }

    println("✅ Успешный вход!\n")
    println("📅 Загрузка расписания...")

    val html = api.getSchedulePage()
    if (html == null) {
        println("❌ Не удалось загрузить расписание!")
        return@runBlocking
    }

    val schedule = parser.parse(html)
    if (schedule == null) {
        println("❌ Ошибка парсинга!")
        return@runBlocking
    }

    // Вывод в консоль
    printSchedule(schedule)
}

fun chekScheduleLocal() = runBlocking {
    val files = listOf(
        "/home/max/Windows-SSD/Users/mamon/Documents/черновик cursor/BGTIschedule/app/src/main/res/Персональный кабинет студента.html",
        "/home/max/Windows-SSD/Users/mamon/Documents/черновик cursor/BGTIschedule/app/src/main/res/Персональный кабинет студента с дср.html"
    )
    val parser = ScheduleParser()
    for (f in files) println(f)
    print("выбор файла:")
    val input = readLine().toString().toInt()
    val file = File(files[input])
    val html = try {
        // Читаем с кодировкой, которая используется на сайте (обычно UTF-8 или windows-1251)
        file.readText(Charsets.UTF_8)
    } catch (e: Exception) {
        println("❌ Ошибка чтения файла: ${e.message}")
        return@runBlocking false
    }

    println("🔍 Парсинг...")
    val schedule = parser.parse(html )

    if (schedule == null) {
        println("❌ Ошибка парсинга!")
        return@runBlocking false
    }

    // Вывод результата
    printSchedule(schedule)
}

private fun printSchedule(schedule: Schedule) {
    println("=" .repeat(60))
    println("Студент: ${schedule.studentFIO.name} ${schedule.studentFIO.surname} ${schedule.studentFIO.patronymic}")
    println("Группа: ${schedule.studentFIO.group}")
    println("Неделя: ${schedule.weekRange}")
    println("=" .repeat(60))

    schedule.lessons.forEach { lesson ->
        println("\n📌 ${lesson.day} | ${lesson.date}")
        println("   Пара: ${lesson.lessonNumber} (${lesson.time})")
        println("   Предмет: ${lesson.subject}")
        println("   Тип: ${lesson.type}")
        println("   Преподаватель: ${lesson.teacher}")
        println("   Аудитория: ${lesson.classroom}")

        if (lesson.topic.isNotEmpty()) {
            println("   Тема: ${lesson.topic}")
        }
        if (lesson.note.isNotEmpty()) {
            println("   примечание: ${lesson.note}")
        }
        if (lesson.noteTime.isNotEmpty()) {
            println("   примечание о времени: ${lesson.noteTime}")
        }
        if ((lesson.estimation.isNotEmpty())) (
            println("   оценка: ${lesson.estimation}")
        )
        println("-" .repeat(60))
    }

    println("\n✅ Всего пар: ${schedule.lessons.size}")
}
