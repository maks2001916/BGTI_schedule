package com.example.bgtischedule.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException

private const val TAG = "UniversityApi"
private val schedulePageUrl = "https://lk.bgti.ru/Default.aspx"
class UniversityApi(
    private val loginPageUrl: String = "https://bgti.ru/Enter/Signin.aspx"
) {
    private val client = OkHttpClient.Builder()
        .cookieJar(CookieJarImpl())
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun login(login: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val loginPageRequest = Request.Builder()
                .url(loginPageUrl)
                .get()
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(loginPageRequest).execute().use { loginPageResponse ->
                val loginPageHtml = loginPageResponse.body?.string().orEmpty()
                if (!loginPageResponse.isSuccessful || loginPageHtml.isBlank()) {
                    Log.w(TAG, "login: failed to load login page, code=${loginPageResponse.code}")
                    return@withContext false
                }

                val ssid = extractHiddenValue(loginPageHtml, "ssid")
                val page = extractHiddenValue(loginPageHtml, "page").ifBlank { "0" }

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
                    .header("User-Agent", USER_AGENT)
                    .build()

                client.newCall(request).execute().use { response ->
                    val finalUrl = response.request.url.toString()
                    Log.d(TAG, "login: finalUrl=$finalUrl, code=${response.code}")

                    if (finalUrl.contains("err=1", ignoreCase = true)) {
                        Log.w(TAG, "login: server returned auth error")
                        return@withContext false
                    }

                    // Успешный вход подтверждаем доступностью кабинета
                    getSchedulePageInternal(schedulePageUrl) != null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "login: network error", e)
            false
        }
    }

    suspend fun getSchedulePage(dateRu: String? = null): String? = withContext(Dispatchers.IO) {
        val url = if (dateRu.isNullOrBlank()) schedulePageUrl
        else "$schedulePageUrl?dt=$dateRu"

        getSchedulePageInternal(url)
    }

    private fun getSchedulePageInternal(url: String = schedulePageUrl): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Referer", loginPageUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                val html = response.body?.string()
                val finalUrl = response.request.url.toString()
                Log.d(TAG, "getSchedulePage: code=${response.code}, url=$finalUrl, length=${html?.length ?: 0}")

                if (response.isSuccessful && html != null && isSchedulePage(html)) {
                    html
                } else {
                    Log.w(TAG, "getSchedulePage: not a schedule page")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "getSchedulePage: network error", e)
            null
        }
    }

    private fun isSchedulePage(html: String): Boolean {
        return html.contains("Персональный кабинет студента", ignoreCase = true) ||
            html.contains("<title>Персональный кабинет студента</title>", ignoreCase = true) ||
            html.contains("class=\"hdweek", ignoreCase = true)
    }

    private fun extractHiddenValue(html: String, name: String): String {
        val pattern = """<input[^>]*name=["']$name["'][^>]*value=["']([^"']*)["']""".toRegex(RegexOption.IGNORE_CASE)
        return pattern.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    private class CookieJarImpl : CookieJar {
        private val cookies = mutableListOf<Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
            newCookies.forEach { newCookie ->
                cookies.removeAll {
                    it.name == newCookie.name &&
                        it.domain == newCookie.domain &&
                        it.path == newCookie.path
                }
            }
            cookies.addAll(newCookies)
            Log.d(TAG, "cookies saved for ${url.host}: ${newCookies.map { it.name }}")
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val matched = cookies.filter { it.matches(url) }
            if (matched.isNotEmpty()) {
                Log.d(TAG, "cookies loaded for ${url.host}: ${matched.map { it.name }}")
            }
            return matched
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
