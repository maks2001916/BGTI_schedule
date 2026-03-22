package com.example.bgtischedule.service

import android.app.Activity
import android.widget.Toast
import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.parser.ScheduleParser

private val logUrl: String = "https://bgti.ru//Enter/Signin.aspx"

class Request {
    val api = UniversityApi(logUrl)


}

suspend fun logIn(
    activity: Activity,
    api: UniversityApi,
    login: String,
    password: String): Schedule?
{
    val isLoggedIn = api.login(login,password)
    val parser = ScheduleParser()
    if (!isLoggedIn) {
        Toast.makeText(activity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
    }

    val html = api.getSchedulePage()
    if (html == null) {
        Toast.makeText(activity, "Не удалось загрузить рассписание", Toast.LENGTH_SHORT).show()
    }


    val schedule = parser.parse(html)
    if (schedule == null) {
        Toast.makeText(activity, "Ошибка парсинга", Toast.LENGTH_SHORT).show()
    }

    return schedule

}