package com.cq.iwa.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_read_meter",
    indices = [Index(value = ["customerCode", "userCode", "taskId", "meterId", "readTime"])],
)
data class HistoryReadMeterEntity(
    @PrimaryKey(autoGenerate = true)
    val tableId: Long = 0,
    val taskId: String,
    val meterId: Int,
    val customerCode: String,
    val userCode: String = "",
    val meterCode: String? = null,
    val address: String? = null,
    val reading: String? = null,
    val remark: String? = null,
    val photos: List<String> = emptyList(),
    val readTime: Long = 0,
    val uploadTime: Long = 0,
)
