package com.cq.iwa.feature.dma.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class DmaStatusFilter { ALL, NORMAL, WARN, SEVERE }

enum class DmaSort(val kind: Int?) {
    STATUS(null),
    KL(1),
    EST_LEAK(2),
    LEAK_RATE(3),
    NRW(4),
    FLOW(5),
}

data class DmaOption(
    val id: Int,
    val name: String,
)

data class DmaFilterOptions(
    val levels: List<DmaOption> = emptyList(),
    val parents: List<DmaOption> = emptyList(),
    val ready: Boolean = false,
)

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

enum class DmaTrendTab { DAY, NIGHT, MIN_NIGHT, RATIO, LEAK, MONTH }

enum class DmaHistoryKind { DAILY, LEAK, NRW, FLOW }

enum class DmaValueTone { DEFAULT, BLUE, PURPLE, NAVY, GREEN, YELLOW, ORANGE, RED }

enum class DmaCompareMode { COMPARE, YOY }

@Serializable
data class DmaPartitionUi(
    val id: String,
    val code: String,
    val name: String,
    val parentName: String,
    val levelName: String,
    val analyzeDate: String,
    val status: Int,
    val supply: String,
    val supplyChange: String,
    val supplyChangeUp: Boolean?,
    val nightFlow: String,
    val nightWindow: String,
    val minNightFlow: String,
    val minNightAt: String,
    val nightRatio: String,
    val nightRatioValue: Double?,
    val kl: String,
    val klValue: Double?,
    val estLeak: String,
    val estLeakOver: Boolean?,
    val leakRate: String,
    val leakRateValue: Double?,
    val leakMonth: String,
    val nrw: String,
    val nrwValue: Double?,
)

private val partitionJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

fun encodeDmaPartition(item: DmaPartitionUi): String = partitionJson.encodeToString(item)

fun decodeDmaPartition(raw: String): DmaPartitionUi? {
    if (raw.isBlank()) return null
    return runCatching { partitionJson.decodeFromString<DmaPartitionUi>(raw) }.getOrNull()
}

data class DmaListUi(
    val dataDate: String = "",
    val nightWindow: String = "",
    val updatedAt: String = "",
    val total: Int = 0,
    val normal: Int = 0,
    val warn: Int = 0,
    val severe: Int = 0,
    val unknown: Int = 0,
    val hasMore: Boolean = false,
    val loadTick: Int = 0,
    val keyword: String = "",
    val status: DmaStatusFilter = DmaStatusFilter.ALL,
    val sort: DmaSort = DmaSort.STATUS,
    val levelNum: Int? = null,
    val levelName: String = "",
    val parentId: Int? = null,
    val parentName: String = "",
    val partitions: List<DmaPartitionUi> = emptyList(),
)

data class DmaTileUi(
    val label: String,
    val value: String,
    val unit: String,
    val tone: DmaValueTone,
    val trend: DmaTrendTab,
)

data class DmaChartLine(
    val name: String,
    val values: List<Float>,
)

data class DmaHistoryCell(
    val label: String,
    val value: String,
    val tone: DmaValueTone = DmaValueTone.DEFAULT,
)

data class DmaHistoryRowUi(
    val title: String,
    val subtitle: String,
    val cells: List<DmaHistoryCell>,
    val meta: String = "",
    val deviceIndex: Int? = null,
)

data class DmaDeviceUi(
    val name: String,
    val direction: String,
    val code: String,
    val caliber: String,
    val typeName: String,
    val online: String,
    val cumulative: String,
    val average: String,
    val maxText: String,
    val minText: String,
    val hours: List<Float>,
)

data class DmaDetailUi(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val levelName: String = "",
    val parentName: String = "",
    val userName: String = "",
    val status: Int = -1,
    val dataDate: String = "",
    val nightWindow: String = "",
    val tiles: List<DmaTileUi> = emptyList(),
    val trend: DmaTrendTab = DmaTrendTab.DAY,
    val trendDays: Int? = 7,
    val trendStart: String = "",
    val trendEnd: String = "",
    val trendYear: Int = 0,
    val years: List<Int> = emptyList(),
    val rangeLabel: String = "",
    val chartLabels: List<String> = emptyList(),
    val chartLines: List<DmaChartLine> = emptyList(),
    val historyKind: DmaHistoryKind = DmaHistoryKind.DAILY,
    val historyDays: Int? = 30,
    val historyStart: String = "",
    val historyEnd: String = "",
    val historyYear: Int = 0,
    val flowDates: List<String> = emptyList(),
    val flowDate: String = "",
    val flowCustom: Boolean = false,
    val historyCaption: String = "",
    val rows: List<DmaHistoryRowUi> = emptyList(),
    val devices: List<DmaDeviceUi> = emptyList(),
    val historyHasMore: Boolean = false,
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
