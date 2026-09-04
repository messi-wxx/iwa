package com.cq.iwa.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "read_meter",
    indices = [
        Index(value = ["customerCode", "userCode", "taskId", "meterId"], unique = true),
        Index(value = ["userCode", "taskId", "state"]),
    ],
)
data class ReadMeterEntity(
    @PrimaryKey(autoGenerate = true)
    val tableId: Long = 0,
    val meterId: Int,
    val taskId: String,
    val customerCode: String,
    val userCode: String = "",
    val readName: String,
    val meterCode: String? = null,
    val address: String? = null,
    val caliber: String? = null,
    val clientName: String? = null,
    val clientCode: String? = null,
    val cellPhone: String? = null,
    val lastRead: String? = null,
    val reading: String? = null,
    val remark: String? = null,
    val sort: Int = 0,
    val groupName: String? = null,
    val extInfo: Map<String, String?> = emptyMap(),
    val photos: List<String> = emptyList(),
    val envPhotos: List<String> = emptyList(),
    val state: Int = 0,
    val readTime: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)
