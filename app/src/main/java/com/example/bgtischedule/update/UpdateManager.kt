package com.example.bgtischedule.update

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpdateManager(
    private val checker: GitHubUpdateChecker = GitHubUpdateChecker(),
    private val installer: ApkUpdateInstaller = ApkUpdateInstaller()
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun currentVersionCode(context: Context): Int {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES
        )
        return info.longVersionCode.toInt()
    }

    fun currentVersionName(context: Context): String {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES
        )
        return info.versionName ?: "?"
    }

    suspend fun checkForUpdate(context: Context): UpdateCheckResult {
        return checker.checkForUpdate(currentVersionCode(context))
    }

    suspend fun downloadAndPrepareInstall(
        context: Context,
        info: UpdateInfo
    ): Result<File> {
        _downloadState.value = DownloadState.Downloading(null)
        val result = installer.downloadApk(context, info) { progress ->
            _downloadState.value = DownloadState.Downloading(progress)
        }
        result.onSuccess { file ->
            _downloadState.value = DownloadState.Ready(file.absolutePath)
        }.onFailure { e ->
            _downloadState.value = DownloadState.Failed(e.message ?: "Ошибка загрузки")
        }
        return result
    }

    fun installDownloadedApk(context: Context, apkFile: File): Result<Unit> {
        return installer.installApk(context, apkFile)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_AUTO_UPDATE_CHECK = "settings_auto_update_check"
        const val KEY_LAST_UPDATE_CHECK_MS = "settings_last_update_check_ms"

        /** Интервал автопроверки — 24 часа */
        const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

        fun shouldRunAutoCheck(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)) return false
            val last = prefs.getLong(KEY_LAST_UPDATE_CHECK_MS, 0L)
            return System.currentTimeMillis() - last >= AUTO_CHECK_INTERVAL_MS
        }

        fun markAutoCheckDone(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_UPDATE_CHECK_MS, System.currentTimeMillis())
                .apply()
        }
    }
}
