package com.cq.iwa.readmeter

import android.widget.ImageView
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import java.io.File

object MeterPhotos {
    private const val FILE_HOST = "https://file.aql.cn/api/file/"

    fun source(path: String): Any {
        if (path.isBlank() || path == "button") return path
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val file = File(path)
        if (file.exists() && file.isFile) return file
        val byName = File(file.name)
        if (byName.exists() && byName.isFile) return byName
        if (!path.contains("/") && !path.contains("\\") && !path.contains(".")) {
            return FILE_HOST + path
        }
        return path
    }

    fun sources(paths: List<String>): List<Any> = paths.map(::source)
}

fun ImageView.bindMeterPhoto(path: String?) {
    clipToOutline = true
    val data = path?.takeIf { it.isNotBlank() && it != "button" }
    if (data == null) {
        dispose()
        setImageDrawable(null)
        isVisible = false
    } else {
        isVisible = true
        load(MeterPhotos.source(data)) {
            crossfade(false)
        }
    }
}

