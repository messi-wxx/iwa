package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneQueryResultDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SceneQueryUi(
    val showCustomerPicker: Boolean = false,
    val customerLabel: String = "选择水司",
    val customerValue: String = "",
    val result: SceneQueryResultDto? = null,
    val hasResult: Boolean = false,
)

@HiltViewModel
class SceneQueryViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(SceneQueryUi())
    val ui: StateFlow<SceneQueryUi> = _ui.asStateFlow()

    var onEnterFunctions: ((SceneQueryResultDto) -> Unit)? = null

    init {
        viewModelScope.launch {
            val aql = withContext(ioDispatcher) { repository.isAqlCompany() }
            _ui.value = _ui.value.copy(showCustomerPicker = aql)
        }
    }

    fun selectCustomer(text: String, value: String) {
        _ui.value = _ui.value.copy(
            customerLabel = text.ifBlank { "选择水司" },
            customerValue = value,
        )
    }

    fun search(code: String) {
        val meterCode = code.trim()
        if (meterCode.isBlank()) {
            showToast("请输入相关信息")
            return
        }
        viewModelScope.launch {
            showLoading()
            val target = if (_ui.value.showCustomerPicker) _ui.value.customerValue else ""
            val result = withContext(ioDispatcher) { repository.queryDevice(meterCode, target) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    _ui.value = _ui.value.copy(result = null, hasResult = false)
                    showToast(result.message)
                }
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) {
                        _ui.value = _ui.value.copy(result = result.data, hasResult = false)
                        showToast("未查到该设备")
                    } else {
                        _ui.value = _ui.value.copy(result = result.data, hasResult = true)
                    }
                }
            }
        }
    }

    fun goFunctions() {
        val result = _ui.value.result
        val hasEdc = result?.edcDeviceInfo != null
        val hasEpo = result?.epoProductInfo != null
        if (!hasEdc && !hasEpo) {
            showToast("暂无现场功能")
            return
        }
        val customerCode = result?.edcDeviceInfo?.customerCode.orEmpty()
        viewModelScope.launch {
            showLoading()
            val tokenResult = withContext(ioDispatcher) { repository.switchCompanyToken(customerCode) }
            hideLoading()
            when (tokenResult) {
                is ApiResult.Error -> showToast(
                    tokenResult.message.ifBlank { "切换token错误" },
                )
                is ApiResult.Success -> onEnterFunctions?.invoke(result)
            }
        }
    }

    fun clearTempToken() {
        repository.clearTempToken()
    }
}
