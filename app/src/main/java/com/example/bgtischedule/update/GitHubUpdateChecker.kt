package com.example.bgtischedule.update

import android.util.Log
import com.example.bgtischedule.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "GitHubUpdateChecker"

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
private data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = ""
)

class GitHubUpdateChecker(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext UpdateCheckResult.Error(
                "Укажите github.repo.owner и github.repo.name в gradle.properties"
            )
        }

        try {
            val url = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "BGTISchedule-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = when (response.code) {
                        404 -> "Релиз не найден. Создайте Release на GitHub с APK."
                        403 -> "Лимит GitHub API. Повторите позже."
                        else -> "GitHub ответил: ${response.code}"
                    }
                    Log.w(TAG, "checkForUpdate: $msg")
                    return@withContext UpdateCheckResult.Error(msg)
                }

                val body = response.body?.string().orEmpty()
                val release = json.decodeFromString<GitHubReleaseDto>(body)
                val remoteVersionCode = parseVersionCodeFromBody(release.body)
                val versionName = parseVersionName(release.body, release.tagName, release.name)
                val apk = findApkAsset(release.assets)

                if (apk == null) {
                    return@withContext UpdateCheckResult.Error(
                        "В релизе нет APK. Прикрепите .apk к GitHub Release."
                    )
                }

                val hasUpdate = when {
                    remoteVersionCode != null ->
                        remoteVersionCode > currentVersionCode
                    else ->
                        isNewerVersionName(
                            current = BuildConfig.VERSION_NAME,
                            remote = versionName
                        )
                }

                if (!hasUpdate) {
                    Log.d(TAG, "checkForUpdate: up to date")
                    return@withContext UpdateCheckResult.UpToDate
                }

                UpdateCheckResult.Available(
                    UpdateInfo(
                        versionName = versionName,
                        versionCode = remoteVersionCode ?: (currentVersionCode + 1),
                        releaseNotes = release.body.trim(),
                        apkDownloadUrl = apk.downloadUrl,
                        apkFileName = apk.name,
                        releasePageUrl = release.htmlUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate failed", e)
            UpdateCheckResult.Error(e.message ?: "Ошибка проверки обновления")
        }
    }

    private fun isConfigured(): Boolean {
        return BuildConfig.GITHUB_OWNER.isNotBlank() &&
                BuildConfig.GITHUB_REPO.isNotBlank()
    }

    private fun parseVersionCodeFromBody(body: String): Int? {
        return Regex("""versionCode\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun parseVersionName(body: String, tagName: String, releaseName: String): String {
        Regex("""versionName\s*[:=]\s*([^\s\n]+)""", RegexOption.IGNORE_CASE)
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it.trim() }

        val fromTag = tagName.removePrefix("v").trim()
        if (fromTag.isNotEmpty()) return fromTag
        return releaseName.ifBlank { tagName }
    }

    private fun isNewerVersionName(current: String, remote: String): Boolean {
        val cur = parseVersionParts(current)
        val rem = parseVersionParts(remote)
        for (i in 0 until maxOf(cur.size, rem.size)) {
            val c = cur.getOrElse(i) { 0 }
            val r = rem.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        return version
            .removePrefix("v")
            .split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
    }

    private fun findApkAsset(assets: List<GitHubAssetDto>): GitHubAssetDto? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        return apks.firstOrNull { it.name.contains("release", ignoreCase = true) }
            ?: apks.firstOrNull { it.name.contains("bgti", ignoreCase = true) }
            ?: apks.firstOrNull()
    }
}
