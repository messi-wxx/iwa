package com.cq.iwa.feature.readmeter.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

@Serializable
data class BookDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,
    val taskName: String = "",
    val lastUpdateTime: String? = null,
    val createTime: String? = null,
    val taskType: Int = 1,
    val createName: String? = null,
)

@Serializable
data class MeterDto(
    val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val taskId: String? = null,
    val meterCode: String? = null,
    val address: String? = null,
    val caliber: String? = null,
    val clientName: String? = null,
    val clientCode: String? = null,
    val cellPhone: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val lastRead: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val reading: String? = null,
    val readName: String? = null,
    val remark: String? = null,
    val sort: Int = 0,
    val groupName: String? = null,
    val extInfo: JsonElement? = null,
    val dictionaryItem: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
) {
    fun extInfoMap(): Map<String, String?> {
        val fromExt = parseExt(extInfo)
        if (fromExt.isNotEmpty()) return fromExt
        return parseDictionaryItem(dictionaryItem)
    }

    private fun parseExt(element: JsonElement?): Map<String, String?> {
        return when (element) {
            null, JsonNull -> emptyMap()
            is JsonObject -> element.mapValues { (_, value) -> value.asExtText() }
            is JsonPrimitive -> parseDictionaryItem(element.content)
            else -> emptyMap()
        }
    }
}

@Serializable
data class ReadModelDto(
    val id: Int,
    val meterCode: String? = null,
    val pictureList: List<String> = emptyList(),
    val reading: JsonElement? = null,
    val remark: String? = null,
    val userCode: String? = null,
    val readTime: Long? = null,
    val taskId: String? = null,
)

@Serializable
data class ErrorMsgDto(
    val text: String = "",
    val value: String = "",
)

@Serializable
data class MeterExtInfoDto(
    val text: String? = null,
    val value: String? = null,
)

internal fun parseDictionaryItem(raw: String?): Map<String, String?> {
    if (raw.isNullOrBlank() || raw == "button") return emptyMap()
    return runCatching {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .parseToJsonElement(raw)
            .jsonObject
            .mapValues { it.value.asExtText() }
    }.getOrDefault(emptyMap())
}

private fun JsonElement.asExtText(): String? {
    return when (this) {
        is JsonNull -> null
        is JsonPrimitive -> contentOrNull
        else -> toString()
    }
}

@Serializable
data class FileDto(
    val guid: String = "",
    val name: String? = null,
    val id: String? = null,
    val upLoadTime: String? = null,
)

@Serializable
data class VersionDto(
    val ver: Int = 0,
    val path: String = "",
    val content: String = "",
)

@Serializable
data class LocationAttachmentDto(
    val attachmentId: String = "",
    val seq: Int = 0,
)

@Serializable
data class MeterLocationDto(
    val linkId: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val attachments: List<LocationAttachmentDto> = emptyList(),
)
