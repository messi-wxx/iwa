package com.cq.iwa.feature.readmeter

enum class MeterPlatform(val key: String) {
    EDC("edc"),
    ITWATER("itwater"),
    BCP("bcp"),
    ;

    companion object {
        fun fromConfig(value: String?): MeterPlatform {
            return entries.firstOrNull { it.key.equals(value?.trim(), ignoreCase = true) } ?: EDC
        }
    }
}

object MeterState {
    const val DELETED = -1
    const val UNREAD = 0
    const val READ = 1
    const val UPLOADED = 2
}
