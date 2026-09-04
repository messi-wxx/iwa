package com.cq.iwa.feature.calibration.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.calibration.data.CalibrationRepository
import com.cq.iwa.feature.calibration.network.AddressResultDto
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
class QueryAddressViewModel @Inject constructor(
    private val repository: CalibrationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<QueryAddressUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<QueryAddressUi>> = _uiState.asStateFlow()

    var onOpenBuilding: ((AddressResultDto?, String) -> Unit)? = null
    var onChooseMultiple: ((List<AddressResultDto>, String) -> Unit)? = null

    private var keyword: String = ""
    private var page = 1
    private var loadingMore = false
    private val items = mutableListOf<PlaceSearchItemUi>()
    private var noMore = false

    fun search(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) {
            showToast("请输入相关信息")
            return
        }
        keyword = text
        page = 1
        noMore = false
        items.clear()
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.searchPlace(text, 1) }
            hideLoading()
            when (result) {
                is ApiResult.Success -> {
                    val devices = result.data.value.mapNotNull { it.text?.takeIf { name -> name.isNotBlank() } }
                    items.clear()
                    items += devices.map { PlaceSearchItemUi(it) }
                    val total = result.data.key?.pageTotal ?: items.size
                    noMore = items.size >= total || devices.isEmpty()
                    page = 2
                    publish()
                    if (devices.isEmpty()) resolvePath(text, allowEmpty = true)
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }

    fun loadMore() {
        if (loadingMore || noMore || keyword.isBlank()) return
        loadingMore = true
        publish()
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { repository.searchPlace(keyword, page) }
            loadingMore = false
            when (result) {
                is ApiResult.Success -> {
                    val devices = result.data.value.mapNotNull { it.text?.takeIf { name -> name.isNotBlank() } }
                    if (devices.isEmpty()) {
                        noMore = true
                    } else {
                        items += devices.map { PlaceSearchItemUi(it) }
                        val total = result.data.key?.pageTotal ?: items.size
                        noMore = items.size >= total
                        page++
                    }
                    publish()
                }
                is ApiResult.Error -> {
                    publish()
                    showToast(result.message)
                }
            }
        }
    }

    fun openItem(text: String) {
        resolvePath(text, allowEmpty = true)
    }

    fun resolvePath(meterCode: String, allowEmpty: Boolean) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getLocationPath(meterCode) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    if (allowEmpty && result.message.contains("未在数据库中立根")) {
                        onOpenBuilding?.invoke(null, meterCode)
                    } else {
                        showToast(result.message)
                    }
                }
                is ApiResult.Success -> handlePathResult(result.data, meterCode, allowEmpty)
            }
        }
    }

    fun openSelected(result: AddressResultDto, meterCode: String, allowEmpty: Boolean) {
        if (result.bookLocations.isNotEmpty()) {
            onOpenBuilding?.invoke(result, meterCode)
        } else if (allowEmpty) {
            onOpenBuilding?.invoke(null, meterCode)
        } else {
            showToast("该表没有导入片区信息")
        }
    }

    private fun handlePathResult(list: List<AddressResultDto>, meterCode: String, allowEmpty: Boolean) {
        if (list.isEmpty()) {
            if (allowEmpty) onOpenBuilding?.invoke(null, meterCode)
            else showToast("该表没有导入片区信息")
            return
        }
        if (list.size == 1) {
            openSelected(list.first(), meterCode, allowEmpty)
            return
        }
        if (allowEmpty) {
            onChooseMultiple?.invoke(list, meterCode) ?: openSelected(list.first(), meterCode, true)
        } else {
            openSelected(list.last(), meterCode, false)
        }
    }

    private fun publish() {
        _uiState.value = UiState.Success(
            QueryAddressUi(
                keyword = keyword,
                items = items.toList(),
                empty = items.isEmpty(),
                loadingMore = loadingMore,
                noMore = noMore,
            ),
        )
    }
}
