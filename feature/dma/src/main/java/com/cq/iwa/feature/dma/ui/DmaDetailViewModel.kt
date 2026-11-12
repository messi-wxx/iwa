package com.cq.iwa.feature.dma.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.dma.data.DmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cq.iwa.feature.dma.network.DmaTrendSeriesDto
import kotlinx.coroutines.withContext

@HiltViewModel
class DmaDetailViewModel @Inject constructor(
    private val dmaRepository: DmaRepository,
) : BaseViewModel() {

    private val pageSize = 10
    private val _ui = MutableStateFlow(DmaDetailUi())
    val ui: StateFlow<DmaDetailUi> = _ui.asStateFlow()
    private var requestJob: Job? = null
    private var requestToken = 0
    private var subareaId: Long = 0
    private var dailyPage = 1
    private var dailyTotal = 0
    private var dailyLocal = false
    private var dailyAll: List<DmaHistoryRowUi> = emptyList()
    private var dailyShown = pageSize

    fun load(partition: DmaPartitionUi) {
        val parsed = partition.id.toLongOrNull() ?: return
        if (subareaId == parsed && _ui.value.tiles.isNotEmpty()) return
        subareaId = parsed
        val end = partition.analyzeDate.ifBlank { yesterday() }
        val year = end.take(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        _ui.value = DmaDetailUi(
            id = partition.id,
            name = partition.name.ifBlank { "分区详情" },
            code = partition.code,
            levelName = partition.levelName,
            parentName = partition.parentName,
            status = partition.status,
            dataDate = end,
            nightWindow = partition.nightWindow,
            tiles = tilesOf(partition),
            trendStart = shift(end, -6),
            trendEnd = end,
            trendYear = year,
            years = listOf(year - 2, year - 1, year),
            historyStart = shift(end, -29),
            historyEnd = end,
            historyYear = year,
            flowDates = listOf(end, shift(end, -1), shift(end, -2)),
            flowDate = end,
        )
        reload(includeTrend = true, includeHistory = true)
    }

    fun refresh() {
        if (subareaId == 0L) return
        reload(includeTrend = true, includeHistory = true)
    }

    fun setTrend(tab: DmaTrendTab) {
        if (tab == _ui.value.trend) return
        _ui.value = _ui.value.copy(trend = tab)
        reload(includeTrend = true)
    }

    fun stepTrend(step: Int) {
        val tabs = DmaTrendTab.entries
        val index = tabs.indexOf(_ui.value.trend) + step
        if (index !in tabs.indices) return
        setTrend(tabs[index])
    }

    fun setTrendDays(days: Int) {
        val end = anchor()
        _ui.value = _ui.value.copy(trendDays = days, trendStart = shift(end, -(days - 1)), trendEnd = end)
        reload(includeTrend = true)
    }

    fun setTrendRange(start: String, end: String) {
        val range = clip(start, end) ?: return
        _ui.value = _ui.value.copy(trendDays = null, trendStart = range.first, trendEnd = range.second)
        reload(includeTrend = true)
    }

    fun setTrendYear(index: Int) {
        val year = _ui.value.years.getOrNull(index) ?: return
        if (year == _ui.value.trendYear && _ui.value.trend == DmaTrendTab.MONTH) {
            _ui.value = _ui.value.copy(trendYear = year)
            return
        }
        _ui.value = _ui.value.copy(trendYear = year)
        if (_ui.value.trend == DmaTrendTab.MONTH) reload(includeTrend = true)
    }

    fun setHistory(kind: DmaHistoryKind) {
        if (kind == _ui.value.historyKind) return
        _ui.value = _ui.value.copy(historyKind = kind)
        reload(includeHistory = true)
    }

    fun setHistoryDays(days: Int) {
        val end = anchor()
        _ui.value = _ui.value.copy(historyDays = days, historyStart = shift(end, -(days - 1)), historyEnd = end)
        reload(includeHistory = true)
    }

    fun setHistoryRange(start: String, end: String) {
        val range = clip(start, end) ?: return
        _ui.value = _ui.value.copy(historyDays = null, historyStart = range.first, historyEnd = range.second)
        reload(includeHistory = true)
    }

    fun setHistoryYear(index: Int) {
        val year = _ui.value.years.getOrNull(index) ?: return
        _ui.value = _ui.value.copy(historyYear = year)
        if (_ui.value.historyKind == DmaHistoryKind.LEAK || _ui.value.historyKind == DmaHistoryKind.NRW) {
            reload(includeHistory = true)
        }
    }

    fun setFlowPreset(index: Int) {
        val date = _ui.value.flowDates.getOrNull(index) ?: return
        _ui.value = _ui.value.copy(flowDate = date, flowCustom = false)
        reload(includeHistory = true)
    }

    fun setFlowDate(day: String) {
        val end = anchor()
        val date = if (day > end) end else day
        _ui.value = _ui.value.copy(flowDate = date, flowCustom = true)
        reload(includeHistory = true)
    }

    fun loadMoreHistory() {
        if (_ui.value.historyKind != DmaHistoryKind.DAILY || !_ui.value.historyHasMore) return
        if (dailyLocal) {
            dailyShown += pageSize
            publishDaily()
            return
        }
        dailyPage += 1
        reload(includeHistory = true, appendDaily = true)
    }

    private fun reload(includeTrend: Boolean = false, includeHistory: Boolean = false, appendDaily: Boolean = false) {
        val token = ++requestToken
        requestJob?.cancel()
        showLoading()
        requestJob = viewModelScope.launch {
            try {
                if (includeTrend) loadTrend()
                if (includeHistory) loadHistory(appendDaily)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.message ?: "加载失败")
            } finally {
                if (token == requestToken) hideLoading()
            }
        }
    }

    private suspend fun loadTrend() {
        val ui = _ui.value
        when (ui.trend) {
            DmaTrendTab.DAY -> publishSeries(
                withContext(ioDispatcher) { dmaRepository.flowTrend(subareaId, ui.trendStart, ui.trendEnd) },
            )
            DmaTrendTab.NIGHT -> publishSeriesList(
                withContext(ioDispatcher) { dmaRepository.nightFlowTrend(subareaId, ui.trendStart, ui.trendEnd) },
            )
            DmaTrendTab.MIN_NIGHT -> publishSeries(
                withContext(ioDispatcher) { dmaRepository.minNightFlowTrend(subareaId, ui.trendStart, ui.trendEnd) },
            )
            DmaTrendTab.RATIO -> publishSeriesList(
                withContext(ioDispatcher) { dmaRepository.nightFlowAndKlRateTrend(subareaId, ui.trendStart, ui.trendEnd) },
            )
            DmaTrendTab.LEAK -> publishSeries(
                withContext(ioDispatcher) { dmaRepository.elFlowTrend(subareaId, ui.trendStart, ui.trendEnd) },
            )
            DmaTrendTab.MONTH -> publishSeriesList(
                withContext(ioDispatcher) { dmaRepository.leakageRateTrend(subareaId, ui.trendYear) },
                monthly = true,
            )
        }
    }

    private fun publishSeries(result: ApiResult<DmaTrendSeriesDto>) {
        when (result) {
            is ApiResult.Success -> {
                val (labels, lines) = listOf(result.data).toChartLines()
                publishChart(labels, lines, trendCaption(labels))
            }
            is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
        }
    }

    private fun publishSeriesList(result: ApiResult<List<DmaTrendSeriesDto>>, monthly: Boolean = false) {
        when (result) {
            is ApiResult.Success -> {
                val (labels, lines) = result.data.toChartLines()
                val caption = if (monthly) {
                    "${_ui.value.trendYear}年 · 共${labels.size}个账期"
                } else {
                    trendCaption(labels)
                }
                publishChart(labels, lines, caption)
            }
            is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
        }
    }

    private fun publishChart(labels: List<String>, lines: List<DmaChartLine>, caption: String) {
        _ui.value = _ui.value.copy(chartLabels = labels, chartLines = lines, rangeLabel = caption)
    }

    private fun trendCaption(labels: List<String>): String {
        val ui = _ui.value
        return if (labels.isEmpty()) {
            "${shortDay(ui.trendStart)}~${shortDay(ui.trendEnd)}"
        } else {
            "${labels.first()}~${labels.last()} · 共${labels.size}天"
        }
    }

    private suspend fun loadHistory(append: Boolean) {
        when (_ui.value.historyKind) {
            DmaHistoryKind.DAILY -> loadDaily(append)
            DmaHistoryKind.LEAK -> loadYearDetail(leak = true)
            DmaHistoryKind.NRW -> loadYearDetail(leak = false)
            DmaHistoryKind.FLOW -> loadDevices()
        }
    }

    private suspend fun loadDaily(append: Boolean) {
        if (!append) {
            dailyPage = 1
            dailyShown = pageSize
        }
        val ui = _ui.value
        val result = withContext(ioDispatcher) {
            dmaRepository.flowDetail(subareaId, ui.historyStart, ui.historyEnd, pageSize, dailyPage)
        }
        when (result) {
            is ApiResult.Success -> {
                val page = result.data
                val startIndex = if (append) dailyAll.size else 0
                val rows = flowDetailRows(page.value, ui.historyEnd, startIndex)
                val total = page.key.pageTotal
                if (total > rows.size) {
                    dailyLocal = false
                    dailyTotal = total
                    dailyAll = if (append) dailyAll + rows else rows
                    _ui.value = _ui.value.copy(
                        rows = dailyAll,
                        devices = emptyList(),
                        historyHasMore = dailyAll.size < dailyTotal,
                        historyCaption = "${ui.historyStart} ~ ${ui.historyEnd} · 共${dailyTotal}天",
                    )
                } else {
                    dailyLocal = true
                    dailyAll = rows
                    publishDaily()
                }
            }
            is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
        }
    }

    private fun publishDaily() {
        val ui = _ui.value
        val shown = dailyAll.take(dailyShown)
        _ui.value = ui.copy(
            rows = shown,
            devices = emptyList(),
            historyHasMore = shown.size < dailyAll.size,
            historyCaption = "${ui.historyStart} ~ ${ui.historyEnd} · 共${dailyAll.size}天",
        )
    }

    private suspend fun loadYearDetail(leak: Boolean) {
        val year = _ui.value.historyYear
        val dataDate = _ui.value.dataDate
        if (leak) {
            when (val result = withContext(ioDispatcher) { dmaRepository.leakageRateDetail(subareaId, year) }) {
                is ApiResult.Success -> publishYear(leakageRows(result.data, dataDate), year)
                is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
            }
        } else {
            when (val result = withContext(ioDispatcher) { dmaRepository.nonRevenueWaterDetail(subareaId, year) }) {
                is ApiResult.Success -> publishYear(nrwRows(result.data, dataDate), year)
                is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
            }
        }
    }

    private fun publishYear(rows: List<DmaHistoryRowUi>, year: Int) {
        val unsettled = if (rows.any { it.subtitle == "未结算" }) " · 含当月未结算" else ""
        _ui.value = _ui.value.copy(
            rows = rows,
            devices = emptyList(),
            historyHasMore = false,
            historyCaption = "${year}年 · 共${rows.size}个账期$unsettled",
        )
    }

    private suspend fun loadDevices() {
        val date = _ui.value.flowDate
        val result = withContext(ioDispatcher) { dmaRepository.deviceFlow(subareaId, date) }
        when (result) {
            is ApiResult.Success -> {
                val devices = result.data.map { it.toDeviceUi() }
                _ui.value = _ui.value.copy(
                    devices = devices,
                    rows = devices.mapIndexed { index, device -> device.toRow(index) },
                    historyHasMore = false,
                    historyCaption = "$date · 进出口流量点 ${devices.size} 个 · 点击查看24h曲线",
                )
            }
            is ApiResult.Error -> showToast(result.message.ifBlank { "加载失败" })
        }
    }

    private fun DmaDeviceUi.toRow(index: Int): DmaHistoryRowUi {
        val meta = listOf(code, caliber, typeName).filter { it.isNotBlank() }.joinToString(" · ")
        return DmaHistoryRowUi(
            title = direction.ifBlank { "流量点" },
            subtitle = name,
            meta = meta,
            deviceIndex = index,
            cells = listOf(
                DmaHistoryCell("日累计流量", cumulative.ifBlank { "—" }),
                DmaHistoryCell("平均瞬时", average.ifBlank { "—" }),
                DmaHistoryCell("最大瞬时", maxText.ifBlank { "—" }),
                DmaHistoryCell("最小瞬时", minText.ifBlank { "—" }),
            ),
        )
    }

    private fun anchor(): String = _ui.value.dataDate.ifBlank { yesterday() }

    private fun clip(start: String, end: String): Pair<String, String>? {
        if (start.isBlank() || end.isBlank()) return null
        var from = start
        var to = end
        if (from > to) {
            val swap = from
            from = to
            to = swap
        }
        val limit = anchor()
        if (to > limit) to = limit
        val maxStart = shift(to, -365)
        if (from < maxStart) {
            showToast("自定义范围最长366天，已截取近366天")
            from = maxStart
        }
        return from to to
    }

    private fun yesterday(): String = shift(today(), -1)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Calendar.getInstance().time)

    private fun shift(day: String, offset: Int): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val calendar = Calendar.getInstance()
        calendar.time = runCatching { format.parse(day) }.getOrNull() ?: return day
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return format.format(calendar.time)
    }

    private fun shortDay(day: String): String = if (day.length >= 10) day.substring(5) else day
}
