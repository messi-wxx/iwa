package com.cq.iwa.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "replace_meter",
    indices = [
        Index(value = ["customerCode", "userCode", "taskId", "meterId"], unique = true),
        Index(value = ["userCode", "taskId", "state", "progress"]),
    ],
)
data class ReplaceMeterEntity(
    @PrimaryKey(autoGenerate = true)
    val tableId: Long = 0,
    val meterId: Int,
    val taskId: String,
    val customerCode: String,
    val userCode: String,
    val clientCode: String? = null,
    val address: String? = null,
    val caliber: Int? = null,
    val oldMeterCode: String? = null,
    val oldReading: String? = null,
    val newMeterCode: String? = null,
    val newReading: String? = null,
    val replaceName: String? = null,
    val sort: Int = 0,
    val isReplace: Int = 1,
    val state: Int = 2,
    val progress: Int = 0,
    val oldPhotos: List<String> = emptyList(),
    val newPhotos: List<String> = emptyList(),
    val envPhotos: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val replaceRyFlux: String? = null,
    val extInfo: String? = null,
    val installType: String? = null,
    val verifyOrg: String? = null,
    val verifyDate: String? = null,
    val verifyExpireDate: String? = null,
    val locationPending: Boolean = false,
)
