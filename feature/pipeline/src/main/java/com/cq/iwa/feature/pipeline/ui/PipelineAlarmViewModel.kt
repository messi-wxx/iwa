package com.cq.iwa.feature.pipeline.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.pipeline.data.PipelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.cq.iwa.feature.pipeline.network.PipelineAlarmRecordDto
import com.cq.iwa.feature.pipeline.network.PipelineRecordParam
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PipelineAlarmUi(
    val items: List<PipelineAlarmRecordDto> = emptyList(),
    val empty: Boolean = false,
    val hasMore: Boolean = true,
    val startDate: String = "",
    val endDate: String = "",
    val status: List<Int> = listOf(1),
    val version: Int = 0,
)

@HiltViewModel
class PipelineAlarmViewModel @Inject constructor(
    private val repository: PipelineRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val pageSize = 10
    private var page = 1
    private var iotId: String = ""
    private var siteType: Int = -1
    private var debounce: Job? = null
    private var loadJob: Job? = null

    private val _ui = MutableStateFlow(PipelineAlarmUi())
    val ui: StateFlow<PipelineAlarmUi> = _ui.asStateFlow()

    fun setup(iotId: String?, siteType: Int, start: String, end: String) {
        this.iotId = iotId.orEmpty()
        this.siteType = siteType
        _ui.value = _ui.value.copy(startDate = start, endDate = end)
        refresh()
    }

    fun setDates(start: String, end: String) {
        _ui.value = _ui.value.copy(startDate = start, endDate = end)
        refresh()
    }

    fun toggleStatus(status: Int, checked: Boolean) {
        val next = _ui.value.status.toMutableList()
        if (checked) {
            if (status !in next) next.add(status)
        } else {
            next.remove(status)
        }
        _ui.value = _ui.value.copy(status = next)
        debounce?.cancel()
        debounce = viewModelScope.launch {
            delay(500)
            refresh()
        }
    }

    fun refresh(overlay: Boolean = true) {
        page = 1
        load(false, overlay)
    }

    fun loadMore() {
        if (!_ui.value.hasMore) return
        page++
        load(true, overlay = false)
    }

    private fun load(more: Boolean, overlay: Boolean = !more) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (overlay) showLoading()
            try {
                val param = PipelineRecordParam(
                    startTime = "${_ui.value.startDate}T00:00:00",
                    endTime = "${_ui.value.endDate}T23:59:59",
                    statusList = _ui.value.status,
                    iotId = iotId,
                    entityType = when (siteType) {
                        1 -> 2
                        2, 3 -> 1
                        else -> null
                    },
                )
                val result = withContext(ioDispatcher) { repository.getAlarmRecords(param, pageSize, page) }
                when (result) {
                    is ApiResult.Error -> {
                        if (!more) showToast(result.message)
                        if (more) page--
                        _ui.value = _ui.value.copy(version = _ui.value.version + 1)
                    }
                    is ApiResult.Success -> {
                        val pageItems = result.data.value
                        val items = if (more) _ui.value.items + pageItems else pageItems
                        _ui.value = _ui.value.copy(
                            items = items,
                            empty = items.isEmpty(),
                            hasMore = pageItems.size >= pageSize,
                            version = _ui.value.version + 1,
                        )
                    }
                }
            } finally {
                hideLoading()
            }
        }
    }
}
