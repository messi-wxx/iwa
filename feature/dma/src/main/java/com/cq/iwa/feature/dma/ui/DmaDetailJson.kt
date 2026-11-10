package com.cq.iwa.feature.dma.ui

import com.cq.iwa.feature.dma.network.DmaFlowDayDto
import com.cq.iwa.feature.dma.network.DmaTrendSeriesDto
import java.text.DecimalFormat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

private val dateKeys = listOf(
    "date", "readDate", "analyzeDate", "statDate", "lastAnalyzeDate",
    "day", "time", "month", "yearMonth", "billMonth",
)

private val idKeys = setOf(
    "id", "levelNum", "state", "pageSize", "currentPage", "pageTotal",
    "parentId", "minNightHour", "subareaId",
)

internal data class DmaPoint(
    val label: String,
    val values: Map<String, Double>,
    val texts: Map<String, String> = emptyMap(),
)

internal data class DmaPointPage(
    val points: List<DmaPoint>,
    val pageTotal: Int?,
)

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

internal fun parsePointPage(element: JsonElement): DmaPointPage {
    return DmaPointPage(points = parsePoints(element), pageTotal = findPageTotal(element))
}

internal fun parseDevices(element: JsonElement): List<DmaDeviceUi> {
    val objects = mutableListOf<JsonObject>()
    collectObjects(element, objects)
    val devices = objects.mapNotNull { it.toDevice() }
    if (devices.isNotEmpty()) return devices
    val hours = parsePoints(element).mapNotNull { it.pick(listOf("value", "flow", "instantFlow", "hourFlow")) }
    if (hours.isEmpty()) return emptyList()
    return listOf(hours.toDevice("流量点"))
}

internal fun chartOf(tab: DmaTrendTab, points: List<DmaPoint>): Pair<List<String>, List<DmaChartLine>> {
    val labels = points.map { axisLabel(it.label, tab == DmaTrendTab.MONTH) }
    val specs = when (tab) {
        DmaTrendTab.DAY -> listOf("日供水量" to listOf("flow", "dayFlow", "supply", "value"))
        DmaTrendTab.NIGHT -> listOf(
            "夜间总流量" to listOf("totalNightFlow", "nightFlow", "flow", "value"),
            "夜间最小流量" to listOf("minNightFlow", "minNight"),
        )
        DmaTrendTab.MIN_NIGHT -> listOf("夜间最小流量" to listOf("minNightFlow", "minNight", "value"))
        DmaTrendTab.RATIO -> listOf(
            "夜间流量占比" to listOf("nightFlowRate", "ratio"),
            "KL值" to listOf("klRate", "kl"),
        )
        DmaTrendTab.LEAK -> listOf("日预估漏损量" to listOf("estimatedLeakageFlow", "elFlow", "leakageFlow", "value"))
        DmaTrendTab.MONTH -> listOf(
            "漏损率" to listOf("leakageRate", "lastMonthLeakageRate", "leakRate"),
            "产销差率" to listOf("nonRevenueWaterRate", "lastMonthNonRevenueWaterRate", "nrwRate"),
        )
    }
    val lines = specs.mapNotNull { (name, keys) ->
        if (points.none { it.pick(keys) != null }) return@mapNotNull null
        DmaChartLine(name, points.map { it.pick(keys) ?: 0f })
    }
    return labels to lines
}

internal fun leakRows(points: List<DmaPoint>): List<DmaHistoryRowUi> {
    return points.map { point ->
        val rate = point.pick(listOf("leakageRate", "lastMonthLeakageRate", "leakRate"))
        DmaHistoryRowUi(
            title = axisLabel(point.label, monthly = true),
            subtitle = point.text("settleState", "statusName").ifBlank { "账期" },
            cells = listOf(
                cell("供水量", point, listOf("supply", "flow", "supplyVolume"), volume = true),
                cell("漏损量", point, listOf("leakageVolume", "leakVolume", "lossVolume"), volume = true),
                cell("漏损率", point, listOf("leakageRate", "lastMonthLeakageRate", "leakRate"), percent = true, tone = leakTone(rate)),
                DmaHistoryCell("评定等级", point.text("grade", "gradeName").ifBlank { gradeOf(rate) }, leakTone(rate)),
            ),
        )
    }
}

internal fun nrwRows(points: List<DmaPoint>): List<DmaHistoryRowUi> {
    return points.map { point ->
        val rate = point.pick(listOf("nonRevenueWaterRate", "lastMonthNonRevenueWaterRate", "nrwRate"))
        DmaHistoryRowUi(
            title = axisLabel(point.label, monthly = true),
            subtitle = point.text("settleState", "statusName").ifBlank { "账期" },
            cells = listOf(
                cell("供水量", point, listOf("supply", "flow", "supplyVolume"), volume = true),
                cell("售水量", point, listOf("saleVolume", "soldVolume", "sales"), volume = true),
                cell("产销差量", point, listOf("nrwVolume", "nonRevenueVolume", "diffVolume"), volume = true),
                cell("产销差率", point, listOf("nonRevenueWaterRate", "lastMonthNonRevenueWaterRate", "nrwRate"), percent = true, tone = leakTone(rate)),
                DmaHistoryCell("评定等级", point.text("grade", "gradeName").ifBlank { gradeOf(rate) }, leakTone(rate)),
            ),
        )
    }
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

private fun parsePoints(element: JsonElement): List<DmaPoint> {
    val groups = mutableListOf<List<DmaPoint>>()
    collectPointGroups(element, groups)
    return groups.maxByOrNull { it.size }.orEmpty().sortedBy { it.label }
}

private fun collectPointGroups(element: JsonElement, out: MutableList<List<DmaPoint>>) {
    when (element) {
        is JsonArray -> {
            val points = element.mapNotNull { (it as? JsonObject)?.toPoint() }
            if (points.isNotEmpty()) out += points
            element.forEach { collectPointGroups(it, out) }
        }
        is JsonObject -> element.values.forEach { collectPointGroups(it, out) }
        else -> Unit
    }
}

private fun JsonObject.toPoint(): DmaPoint? {
    val label = dateKeys.firstNotNullOfOrNull { text(it) }?.substringBefore('T') ?: return null
    if (label.isBlank()) return null
    val numbers = mutableMapOf<String, Double>()
    val texts = mutableMapOf<String, String>()
    entries.forEach { (key, value) ->
        val number = value.asDouble()
        if (number != null) {
            numbers[key] = number
        } else {
            val text = (value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
            if (text != null) texts[key] = text
        }
    }
    if (numbers.isEmpty()) return null
    return DmaPoint(label, numbers, texts)
}

private fun findPageTotal(element: JsonElement): Int? {
    if (element is JsonObject && element.containsKey("pageTotal") && element.containsKey("currentPage")) {
        return element.intValue("pageTotal")
    }
    return when (element) {
        is JsonObject -> element.values.firstNotNullOfOrNull { findPageTotal(it) }
        is JsonArray -> element.firstNotNullOfOrNull { findPageTotal(it) }
        else -> null
    }
}

private fun collectObjects(element: JsonElement, out: MutableList<JsonObject>) {
    when (element) {
        is JsonArray -> element.forEach { item ->
            if (item is JsonObject) out += item
            collectObjects(item, out)
        }
        is JsonObject -> element.values.forEach { collectObjects(it, out) }
        else -> Unit
    }
}

private fun JsonObject.toDevice(): DmaDeviceUi? {
    val explicit = text("deviceName") ?: text("meterName")
    val name = explicit ?: text("name") ?: return null
    val looksLikeDevice = explicit != null ||
        text("direction") != null || text("dir") != null || text("inOut") != null ||
        text("deviceCode") != null || text("dn") != null || text("caliber") != null
    if (!looksLikeDevice || name.isBlank()) return null
    val hours = hourValues()
    val maxValue = asDouble("maxFlow", "maxValue", "maxInstant")
    val minValue = asDouble("minFlow", "minValue", "minInstant")
    return DmaDeviceUi(
        name = text("deviceName") ?: text("meterName") ?: name,
        direction = directionText(),
        code = text("code") ?: text("deviceCode").orEmpty(),
        caliber = text("dn") ?: text("caliber") ?: text("diameter") ?: intValue("dn")?.let { "DN$it" }.orEmpty(),
        typeName = text("type") ?: text("meterType") ?: text("deviceType").orEmpty(),
        online = onlineText(),
        cumulative = (asDouble("cumulative", "dayFlow", "totalFlow", "flow"))?.volume().orEmpty(),
        average = (asDouble("avg", "average", "avgFlow"))?.volume().orEmpty(),
        maxText = listOfNotNull(maxValue?.volume(), text("maxHour") ?: text("maxTime")).joinToString(" "),
        minText = listOfNotNull(minValue?.volume(), text("minHour") ?: text("minTime")).joinToString(" "),
        hours = hours,
    )
}

private fun List<Float>.toDevice(name: String): DmaDeviceUi {
    val max = withIndex().maxByOrNull { it.value }
    val min = withIndex().minByOrNull { it.value }
    val sum = sum()
    return DmaDeviceUi(
        name = name,
        direction = "",
        code = "",
        caliber = "",
        typeName = "",
        online = "",
        cumulative = "",
        average = if (isEmpty()) "" else (sum / size).volume(),
        maxText = max?.let { "${it.value.volume()} ${hourLabel(it.index)}" }.orEmpty(),
        minText = min?.let { "${it.value.volume()} ${hourLabel(it.index)}" }.orEmpty(),
        hours = this,
    )
}

private fun JsonObject.hourValues(): List<Float> {
    val array = entries.firstNotNullOfOrNull { (key, value) ->
        if (value is JsonArray && key.lowercase() in setOf("hours", "points", "values", "hourflows", "hourflow")) value else null
    } ?: return emptyList()
    return array.mapNotNull { item ->
        when (item) {
            is JsonPrimitive -> item.asDouble()?.toFloat()
            is JsonObject -> item.asDouble("value", "flow", "instantFlow")?.toFloat()
            else -> null
        }
    }
}

private fun JsonObject.directionText(): String {
    val raw = text("direction") ?: text("dir") ?: text("inOut") ?: text("flowDirection").orEmpty()
    return when {
        raw.contains("进") || raw == "1" || raw.equals("in", true) -> "进口"
        raw.contains("出") || raw == "2" || raw.equals("out", true) -> "出口"
        else -> raw
    }
}

private fun JsonObject.onlineText(): String {
    val raw = text("online") ?: text("onlineState") ?: return ""
    return when (raw) {
        "1", "true", "在线" -> "在线"
        "0", "false", "离线" -> "离线"
        else -> raw
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

private fun cell(
    label: String,
    point: DmaPoint,
    keys: List<String>,
    volume: Boolean = false,
    percent: Boolean = false,
    tone: DmaValueTone = DmaValueTone.DEFAULT,
): DmaHistoryCell {
    val number = point.pick(keys)
    val text = when {
        number == null -> "—"
        percent -> "${number.rate()}%"
        volume -> number.volume()
        else -> number.rate()
    }
    return DmaHistoryCell(label, text, if (number == null) DmaValueTone.DEFAULT else tone)
}

private fun DmaPoint.pick(keys: List<String>): Float? {
    keys.forEach { key -> values[key]?.let { return it.toFloat() } }
    if (keys.isNotEmpty()) return null
    return values.entries.firstOrNull { it.key !in idKeys }?.value?.toFloat()
}

private fun DmaPoint.text(vararg keys: String): String {
    return keys.firstNotNullOfOrNull { texts[it] }.orEmpty()
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

private fun hourLabel(index: Int): String = "%02d:00".format(index)

private fun JsonElement.asDouble(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.doubleOrNull ?: primitive.intOrNull?.toDouble() ?: primitive.longOrNull?.toDouble()
        ?: primitive.contentOrNull?.toDoubleOrNull()
}

private fun JsonObject.asDouble(vararg keys: String): Double? {
    return keys.firstNotNullOfOrNull { key -> this[key]?.asDouble() }
}

private fun JsonObject.text(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}

private fun JsonObject.intValue(key: String): Int? = this[key]?.asDouble()?.toInt()

private fun Double.volume(): String = DecimalFormat("#,##0.##").format(this)

private fun Float.volume(): String = toDouble().volume()

private fun Double.rate(): String = DecimalFormat("0.##").format(this)

private fun Float.rate(): String = toDouble().rate()
