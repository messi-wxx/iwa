package com.cq.iwa.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meter_book",
    indices = [Index(value = ["customerCode", "userCode", "taskId"], unique = true)],
)
data class MeterBookEntity(
    @PrimaryKey(autoGenerate = true)
    val tableId: Long = 0,
    val taskId: String,
    val taskName: String,
    val customerCode: String,
    val userCode: String,
    val taskType: Int = 1,
    val taskState: Int = 0,
    val createTime: String? = null,
    val lastUpdateTime: String? = null,
    val fingerprint: String? = null,
    val downloadTime: Long = 0,
)
