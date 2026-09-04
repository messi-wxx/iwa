package com.cq.iwa.feature.pipeline.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PipelineFormat {

    fun formatFloat(srcValue: String?, decimalPlaces: Int): String {
        if (srcValue.isNullOrEmpty()) return ""
        return try {
            val bigDecimal = BigDecimal(srcValue)
            if (decimalPlaces <= 0) {
                bigDecimal.setScale(0, RoundingMode.HALF_UP).toPlainString()
            } else {
                val rounded = bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP)
                DecimalFormat().apply {
                    applyPattern("0.${"0".repeat(decimalPlaces)}")
                    roundingMode = RoundingMode.HALF_UP
                }.format(rounded)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun now(pattern: String): String = SimpleDateFormat(pattern, Locale.CHINA).format(Date())

    fun beforeDays(days: Int, pattern: String = "yyyy-MM-dd"): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat(pattern, Locale.CHINA).format(calendar.time)
    }

    fun fromTimestamp(timestamp: Long, pattern: String = "MM-dd HH:mm"): String {
        if (timestamp <= 0L) return ""
        return SimpleDateFormat(pattern, Locale.CHINA).format(Date(timestamp))
    }

    fun fromIso(iso: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (iso.isBlank()) return ""
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
        )
        formats.forEach { fmt ->
            runCatching {
                val date = SimpleDateFormat(fmt, Locale.CHINA).parse(iso)
                if (date != null) return SimpleDateFormat(pattern, Locale.CHINA).format(date)
            }
        }
        return iso.replace("T", " ")
    }

    fun displayName(name: String?, unit: String?): String {
        val label = name.orEmpty()
        return if (unit.isNullOrEmpty()) label else "$label ($unit)"
    }
}
