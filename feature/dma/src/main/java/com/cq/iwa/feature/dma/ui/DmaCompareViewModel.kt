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
 * 横屏对比、同比页状态。曲线用同一套日、月序列，接口完成后再叠加。
 */
@HiltViewModel
class DmaCompareViewModel @Inject constructor() : BaseViewModel() {

    private val _ui = MutableStateFlow(DmaCompareUi())
    val ui: StateFlow<DmaCompareUi> = _ui.asStateFlow()

    fun load(mode: DmaCompareMode) {
        if (_ui.value.start.isNotBlank() && _ui.value.mode == mode) return
        val end = day(-1)
        val start = day(-7)
        val compareEnd = day(-1 - 365)
        val compareStart = day(-7 - 365)
        _ui.value = _ui.value.copy(
            mode = mode,
            start = start,
            end = end,
            compareStart = compareStart,
            compareEnd = compareEnd,
        )
    }

    fun setMode(mode: DmaCompareMode) {
        if (_ui.value.mode == mode) return
        _ui.value = _ui.value.copy(mode = mode)
    }

    fun setMetric(metric: DmaMetric) {
        _ui.value = _ui.value.copy(metric = metric)
    }

    fun setRange(start: String, end: String) {
        _ui.value = _ui.value.copy(start = start, end = end)
    }

    fun setCompareRange(start: String, end: String) {
        _ui.value = _ui.value.copy(compareStart = start, compareEnd = end)
    }

    fun selectPartition(left: Boolean, name: String) {
        _ui.value = if (left) {
            _ui.value.copy(leftName = name)
        } else {
            _ui.value.copy(rightName = name)
        }
    }

    private fun day(offset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
