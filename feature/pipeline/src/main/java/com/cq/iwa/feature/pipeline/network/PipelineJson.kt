package com.cq.iwa.feature.pipeline.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PipelineJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    inline fun <reified T> decode(text: String?): T? {
        if (text.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }
}
