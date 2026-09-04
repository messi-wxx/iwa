package com.cq.iwa.feature.pipeline.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.cq.iwa.feature.pipeline.data.PipelineLiveSocket
import com.cq.iwa.feature.pipeline.data.PipelineRepository
import com.cq.iwa.feature.pipeline.data.PipelineSessionStore
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.network.PipelineMonitorDto
import com.cq.iwa.feature.pipeline.network.PipelineSiteInfoDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PipelineDetailUi(
    val site: PipelineSiteInfoDto? = null,
    val metrics: List<PipelineMetricDto> = emptyList(),
    val visibleMetrics: List<PipelineMetricDto> = emptyList(),
    val profileName: String = "",
    val credential: String = "",
    val showMoreMetrics: Boolean = false,
    val hasHiddenMetrics: Boolean = false,
)

@HiltViewModel
class PipelineDetailViewModel @Inject constructor(
    private val repository: PipelineRepository,
    private val session: PipelineSessionStore,
    private val liveSocket: PipelineLiveSocket,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(PipelineDetailUi())
    val ui: StateFlow<PipelineDetailUi> = _ui.asStateFlow()

    private val _chart = MutableStateFlow<List<PipelineMonitorDto>>(emptyList())
    val chart: StateFlow<List<PipelineMonitorDto>> = _chart.asStateFlow()

    private var messageJob: Job? = null
    private var siteId: Int = -1

    fun load(id: Int) {
        siteId = id
        session.currentSiteId = id
        viewModelScope.launch {
            showLoading()
            val info = withContext(ioDispatcher) { repository.getSiteInfo(id) }
            val metrics = withContext(ioDispatcher) { repository.getMetricList(id) }
            hideLoading()
            if (info is ApiResult.Error) showToast(info.message)
            if (metrics is ApiResult.Error) showToast(metrics.message)
            val site = (info as? ApiResult.Success)?.data
            val followedIds = session.followList
                .filter { it.siteId == id }
                .flatMap { it.siteMetrics }
                .map { it.id }
                .toSet()
            val list = ((metrics as? ApiResult.Success)?.data.orEmpty()).map { item ->
                item.copy(follow = item.id in followedIds)
            }
            session.currentIotId = site?.iotId
            session.currentSiteType = site?.siteType ?: -1
            _ui.value = PipelineDetailUi(
                site = site,
                metrics = list,
                visibleMetrics = list.filter { it.sort < 99999 },
                hasHiddenMetrics = list.any { it.sort >= 99999 },
            )
            site?.iotId?.let { iotId ->
                val credential = withContext(ioDispatcher) { repository.getCredentials(iotId) }
                _ui.value = _ui.value.copy(credential = credential.orEmpty())
            }
            site?.siteType?.let { type ->
                if (type in 1..3) {
                    val profiles = withContext(ioDispatcher) { repository.getProfiles(type) }
                    if (profiles is ApiResult.Success) {
                        val name = profiles.data.firstOrNull { it.value == site.profileId }?.label.orEmpty()
                        _ui.value = _ui.value.copy(profileName = name)
                    }
                }
            }
            connectAndSubscribe()
        }
    }

    fun toggleMoreMetrics(show: Boolean) {
        val all = _ui.value.metrics
        _ui.value = _ui.value.copy(
            showMoreMetrics = show,
            visibleMetrics = if (show) all else all.filter { it.sort < 99999 },
        )
    }

    fun toggleFollow(metric: PipelineMetricDto) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (metric.follow) repository.cancelFollow(metric.siteId, metric.id)
                else repository.addFollow(metric.siteId, metric.id)
            }
            hideLoading()
            if (result is ApiResult.Error) {
                showToast(result.message)
                return@launch
            }
            val next = _ui.value.metrics.map {
                if (it.id == metric.id) it.copy(follow = !it.follow) else it
            }
            applyMetrics(next)
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

    fun connectAndSubscribe() {
        val url = session.webSocketUrl
        if (url.isNotBlank()) liveSocket.connect(url)
        liveSocket.subscribeMetrics(_ui.value.metrics)
        if (messageJob == null) {
            messageJob = viewModelScope.launch {
                liveSocket.messages.collect { text ->
                    applyMetrics(liveSocket.applyTelemetryToMetrics(text, _ui.value.metrics))
                }
            }
        }
    }

    fun displayValue(metric: PipelineMetricDto): String =
        PipelineFormat.formatFloat(metric.value, metric.digit)

    private fun applyMetrics(metrics: List<PipelineMetricDto>) {
        val show = _ui.value.showMoreMetrics
        _ui.value = _ui.value.copy(
            metrics = metrics,
            visibleMetrics = if (show) metrics else metrics.filter { it.sort < 99999 },
            hasHiddenMetrics = metrics.any { it.sort >= 99999 },
        )
    }
}
