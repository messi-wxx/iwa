package com.cq.iwa.feature.diagnose.network

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosePartInfoDto(
    val id: String? = null,
    val code: String? = null,
    val productIdDesc: DiagnoseProductIdDescDto? = null,
)

@Serializable
data class DiagnoseProductIdDescDto(
    val code: String? = null,
)

@Serializable
data class DiagnoseProductDto(
    val id: String? = null,
    val code: String? = null,
    val propertys: DiagnosePropertyDto? = null,
)

@Serializable
data class DiagnosePropertyDto(
    val InitPulse: String? = null,
    val InitWater: String? = null,
    val ValveState: String? = null,
)

@Serializable
data class UpdateReadingRequestDto(
    val items: List<UpdateWaterItemDto>,
    val appVersion: String,
)

@Serializable
data class UpdateWaterItemDto(
    val code: String,
    val initWater: Float,
    val readTime: String,
)

@Serializable
data class UpdateLogResultDto(
    val isSuccess: Boolean = false,
    val message: String? = null,
)
