package com.cq.iwa.feature.installation.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.installation.data.InstFormat
import com.cq.iwa.feature.installation.data.InstRepository
import com.cq.iwa.feature.installation.network.InstAddMeterBody
import com.cq.iwa.feature.installation.network.InstMeterRecordDto
import com.cq.iwa.feature.installation.network.InstRecordInfoBody
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class InstMeterUi(
    val items: List<InstMeterRecordDto> = emptyList(),
    val empty: Boolean = false,
    val caliber: List<Pair<String, String>> = emptyList(),
    val meterType: List<Pair<String, String>> = emptyList(),
    val factory: List<Pair<String, String>> = emptyList(),
    val direction: List<Pair<String, String>> = emptyList(),
)

@HiltViewModel
class InstMeterViewModel @Inject constructor(
    private val repository: InstRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var projectId: Int = 0
    private val _ui = MutableStateFlow(InstMeterUi())
    val ui: StateFlow<InstMeterUi> = _ui.asStateFlow()

    fun load(meterNo: String? = null, userNo: String? = null, address: String? = null) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.getMeterRecordInfoList(
                    InstRecordInfoBody(
                        projectId = projectId,
                        meterNo = meterNo,
                        userNo = userNo,
                        address = address,
                    ),
                )
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    items = result.data.value,
                    empty = result.data.value.isEmpty(),
                )
            }
        }
    }

    fun loadOptions() {
        viewModelScope.launch {
            val caliber = withContext(ioDispatcher) { repository.getOptionList("Caliber") }
            val type = withContext(ioDispatcher) { repository.getOptionList("MeterType") }
            val factory = withContext(ioDispatcher) { repository.getOptionList("Factory") }
            val direction = withContext(ioDispatcher) { repository.getOptionList("Direction") }
            _ui.value = _ui.value.copy(
                caliber = (caliber as? ApiResult.Success)?.data?.let { InstFormat.parseOptions(it) }.orEmpty(),
                meterType = (type as? ApiResult.Success)?.data?.let { InstFormat.parseOptions(it) }.orEmpty(),
                factory = (factory as? ApiResult.Success)?.data?.let { InstFormat.parseOptions(it) }.orEmpty(),
                direction = (direction as? ApiResult.Success)?.data?.let { InstFormat.parseOptions(it) }.orEmpty(),
            )
        }
    }

    fun save(body: InstAddMeterBody, isNew: Boolean) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (isNew) repository.postMeter(body) else repository.updateMeter(body)
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast(if (isNew) "新增成功" else "更新成功")
                    load()
                }
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.deleteMeter(id) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    showToast(result.message)
                    load()
                }
                is ApiResult.Success -> {
                    showToast("删除成功")
                    load()
                }
            }
        }
    }

    fun importExcel(path: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.uploadExcel(projectId, File(path)) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message.ifBlank { "请检查导入数据格式是否正确" })
                is ApiResult.Success -> {
                    showToast("导入成功")
                    load()
                }
            }
        }
    }
}
