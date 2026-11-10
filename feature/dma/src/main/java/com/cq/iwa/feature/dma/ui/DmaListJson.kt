package com.cq.iwa.feature.dma.ui

import com.cq.iwa.feature.dma.network.DmaListDto
import com.cq.iwa.feature.dma.network.DmaSubareaDto
import java.text.DecimalFormat
import kotlin.math.abs

internal data class DmaListPage(
    val total: Int,
    val normal: Int,
    val warn: Int,
    val severe: Int,
    val unknown: Int,
    val pageTotal: Int,
    val items: List<DmaPartitionUi>,
)

internal fun parseDmaList(dto: DmaListDto): DmaListPage {
    val stat = dto.stat
    return DmaListPage(
        total = stat.totalCount,
        normal = stat.normalCount,
        warn = stat.warningCount,
        severe = stat.severeCount,
        unknown = stat.unknownCount,
        pageTotal = dto.result.key.pageTotal,
        items = dto.result.value.map { it.toUi() },
    )
}

internal fun DmaSubareaDto.toUi(): DmaPartitionUi {
    val change = yesterdayFlowRate
    return DmaPartitionUi(
        id = id.toString(),
        code = code,
        name = name,
        parentName = parentName.orEmpty(),
        levelName = levelLabel(levelNum),
        analyzeDate = lastAnalyzeDate.orEmpty().substringBefore('T'),
        status = state,
        supply = flow.volume(),
        supplyChange = change?.let { abs(it).rate() }.orEmpty(),
        supplyChangeUp = change?.let { it > 0 },
        nightFlow = totalNightFlow.volume(),
        nightWindow = nightWindow(nightFlowBeginHour, nightFlowEndHour),
        minNightFlow = minNightFlow.volume(),
        minNightAt = minNightHour?.let { "%02d:00".format(it) }.orEmpty(),
        nightRatio = nightFlowRate.rate(),
        nightRatioValue = nightFlowRate,
        kl = klRate.rate(),
        klValue = klRate,
        estLeak = estimatedLeakageFlow.volume(),
        estLeakOver = estimatedLeakageFlow?.let { it > 0 },
        leakRate = lastMonthLeakageRate.rate(),
        leakRateValue = lastMonthLeakageRate,
        leakMonth = monthLabel(lastMonth),
        nrw = lastMonthNonRevenueWaterRate.rate(),
        nrwValue = lastMonthNonRevenueWaterRate,
    )
}

private fun levelLabel(level: Int): String {
    if (level <= 0) return ""
    val names = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    val name = names.getOrNull(level).orEmpty()
    val text = if (name.isEmpty()) "${level}级" else "${name}级"
    return "${text}分区"
}

private fun nightWindow(begin: String?, end: String?): String {
    val start = begin.hourMinute()
    val stop = end.hourMinute()
    return listOf(start, stop).filter { it.isNotBlank() }.joinToString("-")
}

private fun String?.hourMinute(): String {
    val text = this?.trim().orEmpty()
    if (text.length < 5) return text
    return text.substring(0, 5)
}

private fun monthLabel(raw: String?): String {
    val text = raw?.trim().orEmpty()
    if (text.length == 6 && text.all { it.isDigit() }) {
        return "${text.substring(0, 4)}年${text.substring(4)}月"
    }
    if (text.length >= 7 && text[4] == '-') {
        return "${text.substring(0, 4)}年${text.substring(5, 7)}月"
    }
    return text
}

private fun Double?.volume(): String {
    if (this == null) return ""
    return DecimalFormat("#,##0.##").format(this)
}

private fun Double?.rate(): String {
    if (this == null) return ""
    return DecimalFormat("0.##").format(this)
}
