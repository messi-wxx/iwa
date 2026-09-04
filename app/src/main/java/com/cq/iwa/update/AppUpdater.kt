package com.cq.iwa.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * 使用系统 DownloadManager 下载 APK 并唤起安装，对齐老版 IWA。
 */
class AppUpdater private constructor(private val context: Context) {

    private var downloadId: Long = -1
    private var downloadManager: DownloadManager? = null
    private var downloadCompleteReceiver: BroadcastReceiver? = null

    companion object {
        @Volatile
        private var instance: AppUpdater? = null

        fun getInstance(context: Context): AppUpdater {
            return instance ?: synchronized(this) {
                instance ?: AppUpdater(context.applicationContext).also { instance = it }
            }
        }
    }

    fun downloadApk(url: String, fileName: String = "iwa.apk") {
        if (url.isBlank()) return
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("重庆智慧水务")
                setDescription("正在下载新版本")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createDownloadNotificationChannel()
                }
            }
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager?.enqueue(request) ?: -1
            registerDownloadReceiver()
        } catch (_: Exception) {
        }
    }

    private fun createDownloadNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            "download_channel",
            "文件下载",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "文件下载进度通知"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun registerDownloadReceiver() {
        unregisterReceiver()
        downloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager?.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val downloaded = downloadManager?.getUriForDownloadedFile(downloadId)
                            val localIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val fallback = if (localIndex >= 0) cursor.getString(localIndex) else null
                            installApk(downloaded?.toString() ?: fallback.orEmpty())
                        }
                    }
                }
                unregisterReceiver()
            }
        }
        ContextCompat.registerReceiver(
            context,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun installApk(apkUri: String) {
        if (apkUri.isBlank()) return
        try {
            val parsed = Uri.parse(apkUri)
            val contentUri = if (parsed.scheme == "content") {
                parsed
            } else {
                val file = File(parsed.path ?: return)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (_: Exception) {
        }
    }

    private fun unregisterReceiver() {
        downloadCompleteReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            downloadCompleteReceiver = null
        }
    }
}
