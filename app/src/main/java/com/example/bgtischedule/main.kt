package com.example.bgtischedule

import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.parser.ScheduleParser
import kotlinx.coroutines.runBlocking

private val logUrl: String = "https://bgti.ru//Enter/Signin.aspx"
private val sheduleUrl: String = "https://lk.bgti.ru/Default.aspx"



fun main() = runBlocking {
    println("=== Парсер расписания БГТИ ===\n")

    val api = UniversityApi(logUrl) // Укажите реальный URL
    val parser = ScheduleParser()

    // Ввод данных (в реальном приложении будет UI)
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

private fun printSchedule(schedule: Schedule) {
    println("=" .repeat(60))
    println("Студент: ${schedule.studentFIO.name} ${schedule.studentFIO.surname} ${schedule.studentFIO.patronymic}")
    println("Группа: ${schedule.group}")
    println("Неделя: ${schedule.weekRange}")
    println("=" .repeat(60))

    schedule.lessons.forEach { lesson ->
        println("\n📌 ${lesson.day} (${lesson.date})")
        println("   Пара: ${lesson.lessonNumber} (${lesson.time})")
        println("   Предмет: ${lesson.subject}")
        println("   Тип: ${lesson.type}")
        println("   Преподаватель: ${lesson.teacher}")
        println("   Аудитория: ${lesson.classroom}")
        if (lesson.topic.isNotEmpty()) {
            println("   Тема: ${lesson.topic}")
        }
        println("-" .repeat(60))
    }

    println("\n✅ Всего пар: ${schedule.lessons.size}")
}
