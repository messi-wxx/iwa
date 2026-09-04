package com.cq.iwa.feature.calibration.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiEvent
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.calibration.data.CalibrationRepository
import com.cq.iwa.feature.calibration.network.AddressResultDto
import com.cq.iwa.feature.calibration.network.LocationPlaceDto
import com.cq.iwa.feature.calibration.network.LocationRecordDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BuildingLocationViewModel @Inject constructor(
    private val repository: CalibrationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<BuildingLocationUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<BuildingLocationUi>> = _uiState.asStateFlow()

    private var nodes: List<BuildingNodeUi> = emptyList()

    fun loadFromMeter(meterCode: String, requireArea: Boolean) {
        if (meterCode.isBlank()) {
            showToast("请输入表号")
            if (requireArea) closePage()
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getLocationPath(meterCode) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    showToast(result.message)
                    if (requireArea) closePage()
                }
                is ApiResult.Success -> {
                    val chosen = result.data.lastOrNull()
                    if (requireArea && chosen?.bookLocations.isNullOrEmpty()) {
                        showToast("该表没有导入片区信息")
                        closePage()
                        return@launch
                    }
                    bindPath(chosen, meterNodeCode = meterCode, preferLocationNode = requireArea)
                }
            }
        }
    }

    fun bindPath(
        result: AddressResultDto?,
        meterNodeCode: String?,
        preferLocationNode: Boolean = false,
    ) {
        val locations = result?.bookLocations.orEmpty()
        val list = locations.map { item ->
            BuildingNodeUi(
                id = item.id.toLong(),
                name = item.name.orEmpty(),
                remark = item.remark.orEmpty(),
                selected = false,
            )
        }.toMutableList()
        val code = meterNodeCode?.takeIf { it.isNotBlank() }
        if (!code.isNullOrBlank()) {
            list += BuildingNodeUi(
                id = (result?.meterId ?: 0).toLong(),
                name = code,
                remark = "METER",
                selected = false,
            )
        }
        if (list.isEmpty()) {
            showToast("该表没有导入片区信息")
            return
        }
        val selectedIndex = if (preferLocationNode) {
            list.indexOfLast { it.remark != "METER" }.takeIf { it >= 0 } ?: list.lastIndex
        } else {
            list.lastIndex
        }
        nodes = list.mapIndexed { index, node -> node.copy(selected = index == selectedIndex) }
        publish(null)
        loadSelected(nodes[selectedIndex])
    }

    fun select(index: Int) {
        if (index !in nodes.indices) return
        nodes = nodes.mapIndexed { i, node -> node.copy(selected = i == index) }
        publish(null)
        loadSelected(nodes[index])
    }

    fun reloadSelected() {
        nodes.firstOrNull { it.selected }?.let(::loadSelected)
    }

    fun loadHistory(onResult: (List<LocationRecordDto>) -> Unit) {
        val last = nodes.lastOrNull() ?: return
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.queryHistory(last.name, last.id) }
            hideLoading()
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) showToast("暂无历史记录")
                    onResult(result.data)
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }

    fun loadRecord(id: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.queryRecordDetail(id) }
            hideLoading()
            when (result) {
                is ApiResult.Success -> publish(result.data)
                is ApiResult.Error -> {
                    showToast(result.message)
                    publish(null)
                }
            }
        }
    }

    private fun loadSelected(node: BuildingNodeUi) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (node.remark == "METER") {
                    val meter = nodes.lastOrNull { it.remark == "METER" } ?: node
                    repository.getMeterPlace(meter.name, meter.id)
                } else {
                    repository.getLocationPlace(node.id)
                }
            }
            hideLoading()
            when (result) {
                is ApiResult.Success -> publish(result.data)
                is ApiResult.Error -> {
                    showToast(result.message)
                    publish(null)
                }
            }
        }
    }

    private fun publish(place: LocationPlaceDto?) {
        val selected = nodes.firstOrNull { it.selected }
        _uiState.value = UiState.Success(
            BuildingLocationUi(
                nodes = nodes,
                photos = place?.attachments?.map { it.attachmentId }.orEmpty().filter { it.isNotBlank() },
                remark = place?.remark.orEmpty(),
                lat = place?.lat,
                lng = place?.lng,
                selectedId = selected?.id ?: 0,
                selectedIsMeter = selected?.remark == "METER",
                selectedName = selected?.name.orEmpty(),
                pathText = currentPath(),
                hasPlace = place != null && ((place.lat ?: 0.0) > 0 || place.attachments.isNotEmpty() || !place.remark.isNullOrBlank()),
            ),
        )
    }

    fun currentPath(): String {
        val sb = StringBuilder()
        nodes.forEach { node ->
            sb.append(node.name)
            if (!node.selected) sb.append("/") else return sb.toString()
        }
        return sb.toString()
    }

    private fun closePage() {
        sendEvent(UiEvent.Navigate("close"))
    }
}
