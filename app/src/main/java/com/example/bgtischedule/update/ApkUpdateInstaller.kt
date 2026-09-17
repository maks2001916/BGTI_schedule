package com.example.bgtischedule.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

private const val TAG = "ApkUpdateInstaller"

class ApkUpdateInstaller(
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float?) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(updatesDir, info.apkFileName.ifBlank { "update.apk" })

            val request = Request.Builder()
                .url(info.apkDownloadUrl)
                .header("User-Agent", "BGTISchedule-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Не удалось скачать APK: ${response.code}")
                    )
                }

                val body = response.body ?: return@withContext Result.failure(
                    Exception("Пустой ответ при загрузке")
                )

                val total = body.contentLength()
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress(downloaded.toFloat() / total.toFloat())
                            } else {
                                onProgress(null)
                            }
                        }
                    }
                }
            }

            onProgress(1f)
            Log.i(TAG, "downloadApk: saved to ${target.absolutePath}")
            Result.success(target)
        } catch (e: Exception) {
            Log.e(TAG, "downloadApk failed", e)
            Result.failure(e)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "installApk failed", e)
            Result.failure(e)
        }
    }
}
