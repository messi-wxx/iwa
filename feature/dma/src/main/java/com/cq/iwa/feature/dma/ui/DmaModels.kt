package com.cq.iwa.feature.dma.ui

enum class DmaStatusFilter { ALL, NORMAL, WARN, SEVERE }

enum class DmaMetric {
    SUPPLY,
    NIGHT,
    NIGHT_RATIO,
    KL,
    LEAK,
    NRW,
    MNF,
    EST_LEAK,
}

enum class DmaDetailTab { TREND, DAILY, MONTH, FLOW }

enum class DmaCompareMode { COMPARE, YOY }

data class DmaPartitionUi(
    val id: String,
    val name: String,
    val parentName: String,
    val levelName: String,
    val status: Int,
    val supply: String,
    val kl: String,
    val leakRate: String,
    val nightRatio: String,
)

data class DmaMetricCell(
    val metric: DmaMetric,
    val value: String,
)

data class DmaListUi(
    val dataDate: String = "",
    val nightWindow: String = "",
    val updatedAt: String = "",
    val total: Int = 0,
    val normal: Int = 0,
    val warn: Int = 0,
    val severe: Int = 0,
    val keyword: String = "",
    val status: DmaStatusFilter = DmaStatusFilter.ALL,
    val sortName: String = "默认",
    val levelName: String = "全部等级",
    val parentName: String = "全部上级",
    val partitions: List<DmaPartitionUi> = emptyList(),
)

data class DmaDetailUi(
    val id: String = "",
    val name: String = "",
    val dataDate: String = "",
    val nightWindow: String = "",
    val updatedAt: String = "",
    val metrics: List<DmaMetricCell> = emptyList(),
    val tab: DmaDetailTab = DmaDetailTab.TREND,
    val trendStart: String = "",
    val trendEnd: String = "",
    val historyStart: String = "",
    val historyEnd: String = "",
    val monthStart: String = "",
    val monthEnd: String = "",
    val flowDate: String = "",
    val history: List<String> = emptyList(),
    val flowMeters: List<String> = emptyList(),
)

data class DmaCompareUi(
    val mode: DmaCompareMode = DmaCompareMode.COMPARE,
    val metric: DmaMetric = DmaMetric.SUPPLY,
    val leftName: String = "",
    val rightName: String = "",
    val start: String = "",
    val end: String = "",
    val compareStart: String = "",
    val compareEnd: String = "",
    val partitions: List<DmaPartitionUi> = emptyList(),
)
