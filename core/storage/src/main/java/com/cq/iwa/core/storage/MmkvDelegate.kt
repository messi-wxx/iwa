package com.cq.iwa.core.storage

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T> mmkvDelegate(
    key: String,
    default: T,
    mmkv: MMKV = MMKV.defaultMMKV(),
    json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when (T::class) {
            String::class -> mmkv.decodeString(key, default as String) as T
            Int::class -> mmkv.decodeInt(key, default as Int) as T
            Long::class -> mmkv.decodeLong(key, default as Long) as T
            Float::class -> mmkv.decodeFloat(key, default as Float) as T
            Double::class -> mmkv.decodeDouble(key, default as Double) as T
            Boolean::class -> mmkv.decodeBool(key, default as Boolean) as T
            else -> {
                val raw = mmkv.decodeString(key, null) ?: return default
                runCatching { json.decodeFromString(serializer<T>(), raw) }.getOrDefault(default)
            }
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        when (T::class) {
            String::class -> mmkv.encode(key, value as String)
            Int::class -> mmkv.encode(key, value as Int)
            Long::class -> mmkv.encode(key, value as Long)
            Float::class -> mmkv.encode(key, value as Float)
            Double::class -> mmkv.encode(key, value as Double)
            Boolean::class -> mmkv.encode(key, value as Boolean)
            else -> mmkv.encode(key, json.encodeToString(serializer<T>(), value))
        }
    }
}
