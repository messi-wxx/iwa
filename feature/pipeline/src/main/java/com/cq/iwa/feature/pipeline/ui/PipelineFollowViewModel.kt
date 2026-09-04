package com.cq.iwa.feature.pipeline.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.pipeline.data.PipelineLiveSocket
import com.cq.iwa.feature.pipeline.data.PipelineRepository
import com.cq.iwa.feature.pipeline.data.PipelineSessionStore
import com.cq.iwa.feature.pipeline.network.PipelineFollowDeviceDto
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.network.PipelineMonitorDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PipelineFollowUi(
    val items: List<PipelineFollowDeviceDto> = emptyList(),
    val empty: Boolean = false,
)

@HiltViewModel
class PipelineFollowViewModel @Inject constructor(
    private val repository: PipelineRepository,
    private val session: PipelineSessionStore,
    private val liveSocket: PipelineLiveSocket,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(PipelineFollowUi())
    val ui: StateFlow<PipelineFollowUi> = _ui.asStateFlow()

    private val _chart = MutableStateFlow<List<PipelineMonitorDto>>(emptyList())
    val chart: StateFlow<List<PipelineMonitorDto>> = _chart.asStateFlow()

    var readyToOpen: (() -> Unit)? = null
    private var messageJob: Job? = null
    private var orderJob: Job? = null

    fun prepareAndOpen() {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getWebSocketUrl() }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    session.webSocketUrl = result.data
                    readyToOpen?.invoke()
                }
            }
        }
    }

    fun loadFollows() {
        viewModelScope.launch {
            showLoading()
            if (session.webSocketUrl.isBlank()) {
                when (val url = withContext(ioDispatcher) { repository.getWebSocketUrl() }) {
                    is ApiResult.Error -> showToast(url.message)
                    is ApiResult.Success -> session.webSocketUrl = url.data
                }
            }
            val result = withContext(ioDispatcher) { repository.getFollowList() }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    showToast(result.message)
                    _ui.value = PipelineFollowUi(empty = true)
                }
                is ApiResult.Success -> {
                    val marked = result.data.map { site ->
                        site.copy(siteMetrics = site.siteMetrics.map { it.copy(follow = true) })
                    }
                    session.followList = marked
                    _ui.value = PipelineFollowUi(items = marked, empty = marked.isEmpty())
                    connectAndSubscribe()
                }
            }
        }
    }

    fun connectAndSubscribe() {
        val url = session.webSocketUrl
        if (url.isNotBlank()) liveSocket.connect(url)
        val metrics = session.followList.flatMap { it.siteMetrics }
        liveSocket.subscribeMetrics(metrics)
        if (messageJob == null) {
            messageJob = viewModelScope.launch {
                liveSocket.messages.collect { text ->
                    val updated = liveSocket.applyTelemetry(text, session.followList)
                    session.followList = updated
                    _ui.value = PipelineFollowUi(items = updated, empty = updated.isEmpty())
                }
            }
        }
    }

    fun disconnect() {
        messageJob?.cancel()
        messageJob = null
        liveSocket.disconnect()
    }

    fun toggleMetric(siteId: Int, metric: PipelineMetricDto?) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (metric == null) {
                    repository.deleteSite(siteId)
                } else if (metric.follow) {
                    repository.cancelFollow(siteId, metric.id)
                } else {
                    repository.addFollow(siteId, metric.id)
                }
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    if (metric == null) showToast("取消关注成功")
                    loadFollows()
                }
            }
        }
    }

    fun updateOrder(items: List<PipelineFollowDeviceDto>) {
        orderJob?.cancel()
        orderJob = viewModelScope.launch {
            delay(1000)
            session.followList = items
            _ui.value = PipelineFollowUi(items = items, empty = items.isEmpty())
            val result = withContext(ioDispatcher) { repository.updateFollowOrder(items) }
            if (result is ApiResult.Error) showToast(result.message)
            loadFollows()
        }
    }

    fun loadChart(metric: PipelineMetricDto, start: String, end: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getTimeseries(metric, start, end) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    showToast(result.message)
                    _chart.value = emptyList()
                }
                is ApiResult.Success -> _chart.value = result.data
            }
        }
    }
}
