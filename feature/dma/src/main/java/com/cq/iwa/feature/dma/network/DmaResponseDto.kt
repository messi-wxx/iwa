package com.cq.iwa.feature.dma.network

import kotlinx.serialization.Serializable

@Serializable
data class DmaLevelOptionDto(
    val label: String = "",
    val value: Int = 0,
)

@Serializable
data class DmaParentDto(
    val id: Int = 0,
    val code: String = "",
    val name: String = "",
    val childCount: Int = 0,
)

@Serializable
data class DmaDetailDto(
    val id: Long = 0,
    val code: String = "",
    val name: String = "",
    val levelNum: Int = 0,
    val parentName: String? = null,
    val manager: String? = null,
    val nightFlowBeginHour: String? = null,
    val nightFlowEndHour: String? = null,
    val lastAnalyzeDate: String? = null,
    val flow: Double? = null,
    val totalNightFlow: Double? = null,
    val minNightFlow: Double? = null,
    val nightFlowRate: Double? = null,
    val klRate: Double? = null,
    val estimatedLeakageFlow: Double? = null,
    val lastMonthLeakageRate: Double? = null,
    val lastMonthNonRevenueWaterRate: Double? = null,
    val state: Int = 0,
)

@Serializable
data class DmaTrendItemDto(
    val trendItemLabel: String = "",
    val trendItemValue: Double? = null,
)

@Serializable
data class DmaTrendSeriesDto(
    val name: String = "",
    val unit: String = "",
    val items: List<DmaTrendItemDto> = emptyList(),
)

@Serializable
data class DmaFlowDayDto(
    val lastAnalyzeDate: String? = null,
    val flow: Double? = null,
    val totalNightFlow: Double? = null,
    val minNightFlow: Double? = null,
    val nightFlowRate: Double? = null,
    val klRate: Double? = null,
    val estimatedLeakageFlow: Double? = null,
)

@Serializable
data class DmaLeakageMonthDto(
    val theMonth: String = "",
    val supplyFlow: Double? = null,
    val usedFlow: Double? = null,
    val residentUsedFlow: Double? = null,
    val differentialFlow: Double? = null,
    val overallLeakageRate: Double? = null,
    val cjjRate1: Double? = null,
    val cjjRate2: Double? = null,
    val cjjRate3: Double? = null,
    val cjjRate4: Double? = null,
    val cjjRate: Double? = null,
)

@Serializable
data class DmaNrwMonthDto(
    val theMonth: String = "",
    val totalMakeWater: Double? = null,
    val totalSalesWater: Double? = null,
    val nonRevenueWater: Double? = null,
    val nonRevenueWaterRate: Double? = null,
)

@Serializable
data class DmaDeviceHourDto(
    val hour: String = "",
    val flow: Double? = null,
)

@Serializable
data class DmaDeviceFlowDto(
    val monitorDeviceId: Long = 0,
    val code: String = "",
    val name: String = "",
    val caliber: String = "",
    val deviceKind: String = "",
    val actionType: Int = 0,
    val actionTypeText: String = "",
    val flow: Double? = null,
    val averageInstantFlow: Double? = null,
    val minInstantFlowDate: String? = null,
    val minInstantFlow: Double? = null,
    val maxInstantFlowDate: String? = null,
    val maxInstantFlow: Double? = null,
    val details: List<DmaDeviceHourDto> = emptyList(),
)

@Serializable
data class DmaFlowDetailDto(
    val key: DmaPageKeyDto = DmaPageKeyDto(),
    val value: List<DmaFlowDayDto> = emptyList(),
)
