package com.cq.iwa.feature.installation.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object InstJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    inline fun <reified T> decode(text: String?): T? {
        if (text.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }
}
