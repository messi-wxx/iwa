package com.cq.iwa.feature.dma.ui

import com.cq.iwa.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 分区列表页状态。接口未接入时只保留筛选条件，不请求网络、不计算指标。
 */
@HiltViewModel
class DmaListViewModel @Inject constructor() : BaseViewModel() {

    private var source: List<DmaPartitionUi> = emptyList()
    private val _ui = MutableStateFlow(DmaListUi())
    val ui: StateFlow<DmaListUi> = _ui.asStateFlow()

    fun refresh() {
        showToast("分区接口尚未接入")
        publish()
    }

    fun setKeyword(keyword: String) {
        _ui.value = _ui.value.copy(keyword = keyword.trim())
        publish()
    }

    fun setStatus(status: DmaStatusFilter) {
        _ui.value = _ui.value.copy(status = status)
        publish()
    }

    fun setSort(name: String) {
        _ui.value = _ui.value.copy(sortName = name)
        publish()
    }

    fun setLevel(name: String) {
        _ui.value = _ui.value.copy(levelName = name)
        publish()
    }

    fun setParent(name: String) {
        _ui.value = _ui.value.copy(parentName = name)
        publish()
    }

    private fun publish() {
        val current = _ui.value
        _ui.value = current.copy(partitions = applyFilters(current))
    }

    private fun applyFilters(current: DmaListUi): List<DmaPartitionUi> {
        return source.filter { item ->
            val statusOk = when (current.status) {
                DmaStatusFilter.ALL -> true
                DmaStatusFilter.NORMAL -> item.status == 0
                DmaStatusFilter.WARN -> item.status == 1
                DmaStatusFilter.SEVERE -> item.status == 2
            }
            val keywordOk = current.keyword.isBlank() || item.name.contains(current.keyword, ignoreCase = true)
            val levelOk = current.levelName == "全部等级" || item.levelName == current.levelName
            val parentOk = current.parentName == "全部上级" || item.parentName == current.parentName
            statusOk && keywordOk && levelOk && parentOk
        }
    }
}
