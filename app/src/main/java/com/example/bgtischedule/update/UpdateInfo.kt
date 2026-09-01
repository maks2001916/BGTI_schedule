package com.example.bgtischedule.update

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val releasePageUrl: String
)

sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float?) : DownloadState()
    data class Ready(val apkPath: String) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
