package com.cq.iwa.core.logger

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimberPlanter {

    fun plant(context: Context, isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree(context))
        }
    }
}

/**
 * Release 环境：WARN 以上写入本地文件。ERROR 且带 Throwable 时由 app 模块上报 Bugly（如接口超时）。
 */
private class ReleaseTree(
    private val context: Context,
) : Timber.Tree() {

    private val logDir: File by lazy {
        File(context.filesDir, "logs").apply { mkdirs() }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        runCatching {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val level = when (priority) {
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
            val line = "$time [$level] ${tag.orEmpty()}: $message${t?.let { "\n${Log.getStackTraceString(it)}" } ?: ""}\n"
            File(logDir, "app.log").appendText(line)
        }
    }
}
