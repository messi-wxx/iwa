package com.cq.iwa.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user",
    indices = [Index(value = ["customerCode", "code"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val state: Int,
    val name: String,
    val code: String,
    val token: String,
    val customer: String,
    val customerCode: String,
    val password: String,
    val menuJson: String? = null,
    val currentUser: Boolean = false,
)

@Entity(tableName = "user_config")
data class UserConfigEntity(
    @PrimaryKey
    val id: Int,
    val customerId: String?,
    val kind: String?,
    val configName: String?,
    val configValue: String?,
    val seq: Int = 0,
    val description: String? = null,
)
