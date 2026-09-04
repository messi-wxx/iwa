package com.cq.iwa.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.Date

class DateConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

class StringListConverter {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun listToString(list: List<String>?): String {
        return json.encodeToString(listSerializer, list ?: emptyList())
    }
}

class StringMapConverter {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer().nullable)

    @TypeConverter
    fun fromString(value: String?): Map<String, String?> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString(mapSerializer, value) }.getOrDefault(emptyMap())
    }

    @TypeConverter
    fun mapToString(map: Map<String, String?>?): String {
        return json.encodeToString(mapSerializer, map ?: emptyMap())
    }
}
