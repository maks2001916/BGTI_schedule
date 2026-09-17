package com.example.bgtischedule.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bgtischedule.update.DownloadState
import com.example.bgtischedule.update.UpdateInfo

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    downloadState: DownloadState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    val isDownloading = downloadState is DownloadState.Downloading
    val progress = (downloadState as? DownloadState.Downloading)?.progress

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("Доступно обновление ${info.versionName}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Новая версия (код ${info.versionCode}) готова к установке.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = info.releaseNotes.take(800),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Загрузка…",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (downloadState is DownloadState.Failed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = downloadState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDownloadAndInstall,
                enabled = !isDownloading
            ) {
                Text(if (downloadState is DownloadState.Ready) "Установить" else "Скачать и установить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDownloading) {
                Text("Позже")
            }
        }
    )
}
