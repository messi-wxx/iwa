package com.cq.iwa.feature.installation.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import com.cq.iwa.feature.installation.network.InstChildNodeDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object InstFormat {

    fun now(pattern: String): String = SimpleDateFormat(pattern, Locale.CHINA).format(Date())

    fun displayTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.replace("T", " ").take(19)
    }

    fun daysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time)
    }

    fun overdueDuration(child: InstChildNodeDto): String? {
        if (!child.isOverdue || child.dueDate.isNullOrBlank()) return null
        val due = parseMillis(child.dueDate) ?: return null
        val end = parseMillis(child.endTime) ?: System.currentTimeMillis()
        val seconds = (end - due) / 1000
        if (seconds <= 0) return null
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val remain = seconds % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}天")
        if (hours > 0) parts.add("${hours}小时")
        if (minutes > 0) parts.add("${minutes}分")
        if (remain > 0 || parts.isEmpty()) parts.add("${remain}秒")
        return parts.joinToString("")
    }

    fun overdueTag(children: List<InstChildNodeDto>): String? {
        val overdue = children.filter { it.isOverdue }
        if (overdue.isEmpty()) return null
        return if (children.size == 1) {
            overdue.first().let { overdueDuration(it) }?.let { "逾期$it" }
        } else {
            "${overdue.size}人逾期"
        }
    }

    private fun parseMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val text = displayTime(raw)
        val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
        formats.forEach { pattern ->
            runCatching {
                return SimpleDateFormat(pattern, Locale.CHINA).parse(text)?.time
            }
        }
        return null
    }

    fun parseOptions(json: String): List<Pair<String, String>> {
        return runCatching {
            val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val array = element as? JsonArray ?: return emptyList()
            array.map { item ->
                if (item is JsonPrimitive) {
                    val text = item.content
                    text to text
                } else {
                    val obj = item.jsonObject
                    val label = obj["label"]?.jsonPrimitive?.contentOrNull
                        ?: obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: ""
                    val value = obj["value"]?.jsonPrimitive?.contentOrNull
                        ?: obj["code"]?.jsonPrimitive?.contentOrNull
                        ?: ""
                    label to value
                }
            }
        }.getOrDefault(emptyList())
    }

    fun wrapCompleteJson(taskId: String, formJson: String): String {
        val form = JSONObject(formJson)
        return JSONObject().apply {
            put("taskId", taskId)
            put("taskFormData", form)
        }.toString()
    }
}
