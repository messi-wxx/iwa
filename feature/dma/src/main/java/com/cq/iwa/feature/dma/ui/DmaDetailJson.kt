package com.cq.iwa.feature.dma.ui

import com.cq.iwa.feature.dma.network.DmaDeviceFlowDto
import com.cq.iwa.feature.dma.network.DmaFlowDayDto
import com.cq.iwa.feature.dma.network.DmaLeakageMonthDto
import com.cq.iwa.feature.dma.network.DmaNrwMonthDto
import com.cq.iwa.feature.dma.network.DmaTrendSeriesDto
import java.text.DecimalFormat

internal fun List<DmaTrendSeriesDto>.toChartLines(): Pair<List<String>, List<DmaChartLine>> {
    val rawLabels = linkedSetOf<String>()
    forEach { series ->
        series.items.forEach { item ->
            if (item.trendItemLabel.isNotBlank()) rawLabels += item.trendItemLabel
        }
    }
    val lines = map { series ->
        val byLabel = series.items.associate { it.trendItemLabel to (it.trendItemValue ?: 0.0).toFloat() }
        DmaChartLine(
            name = series.name.ifBlank { "趋势" },
            values = rawLabels.map { byLabel[it] ?: 0f },
        )
    }
    return rawLabels.map { axisLabel(it, monthly = false) } to lines
}

internal fun flowDetailRows(items: List<DmaFlowDayDto>, rangeEnd: String, startIndex: Int): List<DmaHistoryRowUi> {
    return items.mapIndexed { index, item ->
        val fromApi = item.lastAnalyzeDate.orEmpty().substringBefore('T').takeIf { it.length >= 10 }
        val day = fromApi ?: shiftDay(rangeEnd, -(startIndex + index))
        DmaHistoryRowUi(
            title = if (day.length >= 10) day.substring(5, 10) else "—",
            subtitle = weekday(day),
            cells = listOf(
                numberCell("日供水量", item.flow, volume = true),
                numberCell("夜间流量", item.totalNightFlow, volume = true),
                numberCell("夜间最小", item.minNightFlow, volume = true),
                numberCell("占比", item.nightFlowRate, percent = true, tone = ratioTone(item.nightFlowRate)),
                numberCell("KL值", item.klRate, percent = true, tone = ratioTone(item.klRate)),
                numberCell(
                    "预估漏损",
                    item.estimatedLeakageFlow,
                    volume = true,
                    tone = leakAmountTone(item.estimatedLeakageFlow),
                ),
            ),
        )
    }
}

internal fun leakageRows(items: List<DmaLeakageMonthDto>, dataDate: String): List<DmaHistoryRowUi> {
    return items.sortedBy { it.theMonth }.map { item ->
        val rate = item.overallLeakageRate
        DmaHistoryRowUi(
            title = axisLabel(item.theMonth, monthly = true),
            subtitle = monthSubtitle(item.theMonth, dataDate),
            cells = listOf(
                numberCell("供水量", item.supplyFlow, volume = true),
                numberCell("漏损量", item.differentialFlow, volume = true),
                numberCell("漏损率", rate, percent = true, tone = leakTone(rate)),
                DmaHistoryCell("评定等级", gradeOf(rate), leakTone(rate)),
            ),
        )
    }
}

internal fun nrwRows(items: List<DmaNrwMonthDto>, dataDate: String): List<DmaHistoryRowUi> {
    return items.sortedBy { it.theMonth }.map { item ->
        val rate = item.nonRevenueWaterRate
        DmaHistoryRowUi(
            title = axisLabel(item.theMonth, monthly = true),
            subtitle = monthSubtitle(item.theMonth, dataDate),
            cells = listOf(
                numberCell("制水量", item.totalMakeWater, volume = true),
                numberCell("售水量", item.totalSalesWater, volume = true),
                numberCell("产销差量", item.nonRevenueWater, volume = true),
                numberCell("产销差率", rate, percent = true, tone = leakTone(rate)),
                DmaHistoryCell("评定等级", gradeOf(rate), leakTone(rate)),
            ),
        )
    }
}

internal fun DmaDeviceFlowDto.toDeviceUi(): DmaDeviceUi {
    val hours = FloatArray(24)
    details.forEach { point ->
        val hour = point.hour.substringBefore(':').toIntOrNull() ?: return@forEach
        if (hour in hours.indices) hours[hour] = (point.flow ?: 0.0).toFloat()
    }
    val dn = when {
        caliber.isBlank() -> ""
        caliber.startsWith("DN", ignoreCase = true) -> caliber
        else -> "DN$caliber"
    }
    return DmaDeviceUi(
        name = name,
        direction = actionTypeText.ifBlank { if (actionType == 0) "进口" else "出口" },
        code = code,
        caliber = dn,
        typeName = deviceKind,
        online = "",
        cumulative = flow?.volume().orEmpty(),
        average = averageInstantFlow?.volume().orEmpty(),
        maxText = instantText(maxInstantFlow, maxInstantFlowDate),
        minText = instantText(minInstantFlow, minInstantFlowDate),
        hours = hours.toList(),
    )
}

internal fun tilesOf(partition: DmaPartitionUi): List<DmaTileUi> {
    return listOf(
        DmaTileUi("日供水量", partition.supply.ifBlank { "—" }, "m³", DmaValueTone.BLUE, DmaTrendTab.DAY),
        DmaTileUi("夜间流量", partition.nightFlow.ifBlank { "—" }, "m³", DmaValueTone.PURPLE, DmaTrendTab.NIGHT),
        DmaTileUi("夜间最小流量", partition.minNightFlow.ifBlank { "—" }, "m³/h", DmaValueTone.NAVY, DmaTrendTab.MIN_NIGHT),
        DmaTileUi("夜间流量占比", partition.nightRatio.ifBlank { "—" }, "%", ratioTone(partition.nightRatioValue), DmaTrendTab.RATIO),
        DmaTileUi("KL值", partition.kl.ifBlank { "—" }, "%", ratioTone(partition.klValue), DmaTrendTab.RATIO),
        DmaTileUi("日预估漏损量", partition.estLeak.ifBlank { "—" }, "m³", leakAmountTone(partition.estLeakOver), DmaTrendTab.LEAK),
        DmaTileUi("上月漏损率", partition.leakRate.ifBlank { "—" }, "%", leakTone(partition.leakRateValue), DmaTrendTab.MONTH),
        DmaTileUi("上月产销差率", partition.nrw.ifBlank { "—" }, "%", leakTone(partition.nrwValue), DmaTrendTab.MONTH),
    )
}

private fun monthSubtitle(month: String, dataDate: String): String {
    val compact = month.filter { it.isDigit() }.take(6)
    val current = dataDate.filter { it.isDigit() }.take(6)
    return if (compact.length == 6 && compact == current) "未结算" else "账期"
}

private fun instantText(value: Double?, time: String?): String {
    val amount = value?.volume().orEmpty()
    val at = time?.takeIf { it.isNotBlank() }.orEmpty()
    return when {
        amount.isBlank() && at.isBlank() -> ""
        at.isBlank() -> amount
        amount.isBlank() -> at
        else -> "$amount（$at）"
    }
}

private fun numberCell(
    label: String,
    value: Double?,
    volume: Boolean = false,
    percent: Boolean = false,
    tone: DmaValueTone = DmaValueTone.DEFAULT,
): DmaHistoryCell {
    val text = when {
        value == null -> "—"
        percent -> "${value.rate()}%"
        volume -> value.volume()
        else -> value.rate()
    }
    return DmaHistoryCell(label, text, if (value == null) DmaValueTone.DEFAULT else tone)
}

private fun ratioTone(value: Number?): DmaValueTone {
    val number = value?.toDouble() ?: return DmaValueTone.NAVY
    return when {
        number > 30 -> DmaValueTone.RED
        number >= 20 -> DmaValueTone.YELLOW
        else -> DmaValueTone.GREEN
    }
}

private fun leakTone(value: Number?): DmaValueTone {
    val number = value?.toDouble() ?: return DmaValueTone.NAVY
    return when {
        number > 15 -> DmaValueTone.RED
        number > 12 -> DmaValueTone.ORANGE
        number >= 9 -> DmaValueTone.YELLOW
        else -> DmaValueTone.GREEN
    }
}

private fun leakAmountTone(value: Number?): DmaValueTone {
    val number = value?.toDouble() ?: return DmaValueTone.NAVY
    return if (number > 0) DmaValueTone.ORANGE else DmaValueTone.GREEN
}

private fun leakAmountTone(over: Boolean?): DmaValueTone = when (over) {
    true -> DmaValueTone.ORANGE
    false -> DmaValueTone.GREEN
    null -> DmaValueTone.NAVY
}

private fun gradeOf(value: Number?): String {
    val number = value?.toDouble() ?: return "—"
    return when {
        number > 15 -> "不合格"
        number > 12 -> "合格"
        number >= 9 -> "良"
        else -> "优"
    }
}

private fun axisLabel(raw: String, monthly: Boolean): String {
    val text = raw.substringBefore('T')
    val compact = text.replace("-", "")
    if (monthly || text.length == 6 || (text.length >= 7 && text.getOrNull(4) == '-' && text.length <= 7)) {
        val ym = compact.take(6)
        if (ym.length == 6 && ym.all { it.isDigit() }) {
            return "${ym.substring(2, 4)}/${ym.substring(4, 6)}"
        }
    }
    return if (text.length >= 10) text.substring(5, 10) else text
}

private fun shiftDay(day: String, offset: Int): String {
    val text = day.substringBefore('T')
    if (text.length < 10) return ""
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
    val calendar = java.util.Calendar.getInstance()
    calendar.time = runCatching { format.parse(text) }.getOrNull() ?: return ""
    calendar.add(java.util.Calendar.DAY_OF_YEAR, offset)
    return format.format(calendar.time)
}

private fun weekday(raw: String): String {
    val text = raw.substringBefore('T')
    if (text.length < 10) return ""
    val parts = text.split("-")
    if (parts.size < 3) return ""
    val year = parts[0].toIntOrNull() ?: return ""
    val month = parts[1].toIntOrNull() ?: return ""
    val day = parts[2].toIntOrNull() ?: return ""
    val calendar = java.util.Calendar.getInstance()
    calendar.set(year, month - 1, day)
    val names = "日一二三四五六"
    return "周${names[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]}"
}

private fun Double.volume(): String = DecimalFormat("#,##0.##").format(this)

private fun Float.volume(): String = toDouble().volume()

private fun Double.rate(): String = DecimalFormat("0.##").format(this)

private fun Float.rate(): String = toDouble().rate()
