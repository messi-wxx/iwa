package com.cq.iwa.feature.dma.network

import kotlinx.serialization.Serializable

@Serializable
data class DmaListDto(
    val stat: DmaListStatDto = DmaListStatDto(),
    val result: DmaListResultDto = DmaListResultDto(),
)

@Serializable
data class DmaListStatDto(
    val totalCount: Int = 0,
    val normalCount: Int = 0,
    val warningCount: Int = 0,
    val severeCount: Int = 0,
    val unknownCount: Int = 0,
)

@Serializable
data class DmaListResultDto(
    val key: DmaPageKeyDto = DmaPageKeyDto(),
    val value: List<DmaSubareaDto> = emptyList(),
)

@Serializable
data class DmaPageKeyDto(
    val pageSize: Int = 0,
    val currentPage: Int = 0,
    val pageTotal: Int = 0,
    val extraInfo: String? = null,
)

@Serializable
data class DmaSubareaDto(
    val id: Long = 0,
    val code: String = "",
    val name: String = "",
    val levelNum: Int = 0,
    val parentName: String? = null,
    val lastAnalyzeDate: String? = null,
    val flow: Double? = null,
    val yesterdayFlowRate: Double? = null,
    val nightFlowBeginHour: String? = null,
    val nightFlowEndHour: String? = null,
    val totalNightFlow: Double? = null,
    val minNightHour: Int? = null,
    val minNightFlow: Double? = null,
    val nightFlowRate: Double? = null,
    val klRate: Double? = null,
    val estimatedLeakageFlow: Double? = null,
    val lastMonthLeakageRate: Double? = null,
    val lastMonthNonRevenueWaterRate: Double? = null,
    val lastMonth: String? = null,
    val state: Int = 0,
)
