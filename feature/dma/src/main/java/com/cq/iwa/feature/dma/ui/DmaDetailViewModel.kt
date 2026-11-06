package com.cq.iwa.feature.dma.ui

import com.cq.iwa.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 分区详情页状态。指标、曲线和明细等接口完成后再填充。
 */
@HiltViewModel
class DmaDetailViewModel @Inject constructor() : BaseViewModel() {

    private val _ui = MutableStateFlow(DmaDetailUi())
    val ui: StateFlow<DmaDetailUi> = _ui.asStateFlow()

    fun load(id: String, name: String) {
        if (_ui.value.id == id && _ui.value.name.isNotBlank()) return
        val end = day(-1)
        val start = day(-7)
        _ui.value = DmaDetailUi(
            id = id,
            name = name.ifBlank { "分区详情" },
            metrics = DmaMetric.entries.map { DmaMetricCell(it, "—") },
            trendStart = start,
            trendEnd = end,
            historyStart = start,
            historyEnd = end,
            monthStart = start,
            monthEnd = end,
            flowDate = end,
        )
    }

    fun refresh() {
        showToast("分区接口尚未接入")
    }

    fun setTab(tab: DmaDetailTab) {
        _ui.value = _ui.value.copy(tab = tab)
    }

    fun setTrendRange(start: String, end: String) {
        _ui.value = _ui.value.copy(trendStart = start, trendEnd = end)
    }

    fun setHistoryRange(start: String, end: String) {
        _ui.value = _ui.value.copy(historyStart = start, historyEnd = end)
    }

    fun setMonthRange(start: String, end: String) {
        _ui.value = _ui.value.copy(monthStart = start, monthEnd = end)
    }

    fun setFlowDate(day: String) {
        _ui.value = _ui.value.copy(flowDate = day)
    }

    fun loadMoreHistory() {
        if (_ui.value.history.isEmpty()) {
            showToast("暂无明细")
        }
    }

    private fun day(offset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
