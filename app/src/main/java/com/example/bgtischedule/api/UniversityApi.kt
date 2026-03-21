package com.example.bgtischedule.api

import okhttp3.*
import java.io.IOException

class UniversityApi(
    private val loginPageUrl: String = "https://bgti.ru/Enter/Signin.aspx"
) {
    private val client = OkHttpClient.Builder()
        .cookieJar(CookieJarImpl()) // Кастомное хранение cookies
        .build()

    suspend fun login(login: String, password: String): Boolean {
        return try {
            // 1) Сначала получаем страницу входа, чтобы забрать hidden-параметры и cookies.
            val loginPageRequest = Request.Builder()
                .url(loginPageUrl)
                .get()
                .build()
            val loginPageResponse = client.newCall(loginPageRequest).execute()
            val loginPageHtml = loginPageResponse.body?.string().orEmpty()
            if (!loginPageResponse.isSuccessful || loginPageHtml.isBlank()) {
                return false
            }

            val ssid = extractHiddenValue(loginPageHtml, "ssid")
            val page = extractHiddenValue(loginPageHtml, "page").ifBlank { "0" }

            // 2) Отправляем форму так же, как это делает браузер.
            val requestBody = FormBody.Builder()
                .add("login", login)
                .add("psw", password)
                .add("ssid", ssid)
                .add("page", page)
                .build()

            val request = Request.Builder()
                .url("https://bgti.ru/Enter/SigninPOST.aspx")
                .post(requestBody)
                .header("Referer", loginPageUrl)
                .build()

            val response = client.newCall(request).execute()

            // После удачного входа кабинет обычно доступен на lk.bgti.ru/Default.aspx
            if (!(response.isSuccessful || response.code in 300..399)) {
                return false
            }
            getSchedulePage() != null
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSchedulePage(): String? {
        return try {
            val request = Request.Builder()
                .url("https://lk.bgti.ru/Default.aspx")
                .get()
                .header("Referer", "https://bgti.ru/Enter/Signin.aspx")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string()
            if (response.isSuccessful &&
                html != null &&
                html.contains("Персональный кабинет студента", ignoreCase = true)
            ) {
                html
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun extractHiddenValue(html: String, name: String): String {
        val pattern = """<input[^>]*name=["']$name["'][^>]*value=["']([^"']*)["']""".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    // Внутренний класс для хранения Cookies в памяти
    private class CookieJarImpl : CookieJar {
        private val cookieStore = HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }
}