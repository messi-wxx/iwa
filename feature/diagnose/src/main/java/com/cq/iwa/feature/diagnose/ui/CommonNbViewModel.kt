package com.cq.iwa.feature.diagnose.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.diagnose.data.DiagnoseRepository
import com.cq.iwa.feature.diagnose.protocol.DiagnoseCommands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CommonNbUi(
    val canWriteWater: Boolean = false,
)

@HiltViewModel
class CommonNbViewModel @Inject constructor(
    private val repository: DiagnoseRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(CommonNbUi())
    val ui: StateFlow<CommonNbUi> = _ui.asStateFlow()

    var onWriteCommand: ((ByteArray) -> Unit)? = null
    var onWriteVerified: ((String) -> Unit)? = null
    var onWriteRollback: ((Int) -> Unit)? = null
    var onBusyChanged: ((Boolean) -> Unit)? = null

    fun loadPermission() {
        launchApi({ repository.canWriteWaterReading() }) { result ->
            if (result is ApiResult.Success) {
                _ui.value = CommonNbUi(canWriteWater = result.data)
            }
        }
    }

    fun prepareModifyWater(meterCode: String, reading: String) {
        viewModelScope.launch {
            onBusyChanged?.invoke(true)
            when (val part = withContext(ioDispatcher) { repository.getPartInfoByCode(meterCode) }) {
                is ApiResult.Error -> {
                    onBusyChanged?.invoke(false)
                    val message = if (part.message.contains("为空")) "未查到成品编号" else part.message
                    showToast(message)
                }
                is ApiResult.Success -> {
                    val productCode = part.data.productIdDesc?.code
                    if (productCode.isNullOrBlank()) {
                        onBusyChanged?.invoke(false)
                        showToast("成品未注册或组件未组装成成品")
                        return@launch
                    }
                    when (val product = withContext(ioDispatcher) {
                        repository.getProductInfoByCode(productCode)
                    }) {
                        is ApiResult.Error -> {
                            onBusyChanged?.invoke(false)
                            showToast(product.message)
                        }
                        is ApiResult.Success -> {
                            if (product.data.propertys == null) {
                                onBusyChanged?.invoke(false)
                                showToast("该水表不支持修改水量")
                            } else {
                                val value = reading.toInt()
                                onWriteCommand?.invoke(DiagnoseCommands.modifyWaterData(value))
                            }
                        }
                    }
                }
            }
        }
    }

    fun onModifyEcho(
        meterCode: String,
        echoed: String?,
        expected: String,
        oldReading: Int,
        appVersion: String,
    ) {
        if (echoed != expected) {
            onBusyChanged?.invoke(false)
            showToast("水表读数写入失败")
            return
        }
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    repository.submitUpdateReading(
                        meterCode = meterCode,
                        initWater = expected.toFloat(),
                        appVersion = appVersion,
                    )
                }
            ) {
                is ApiResult.Success -> {
                    onBusyChanged?.invoke(false)
                    if (result.data.isSuccess) {
                        onWriteVerified?.invoke(echoed)
                        showToast("水表读数提交服务器成功")
                    }
                }
                is ApiResult.Error -> {
                    onWriteCommand?.invoke(DiagnoseCommands.modifyWaterData(oldReading))
                    onWriteRollback?.invoke(oldReading)
                    onBusyChanged?.invoke(false)
                    showToast("提交服务器失败：${result.message}")
                }
            }
        }
    }
}
