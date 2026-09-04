package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.ScenePartIdsDescDto
import com.cq.iwa.feature.sceneservice.network.SceneProductDefineDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SceneUpdateComponentUi(
    val oldParts: List<ScenePartIdsDescDto> = emptyList(),
    val newParts: List<ScenePartIdsDescDto> = emptyList(),
)

@HiltViewModel
class SceneUpdateComponentViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(SceneUpdateComponentUi())
    val ui: StateFlow<SceneUpdateComponentUi> = _ui.asStateFlow()

    var onOpenRegister: ((SceneProductDefineDto, List<String>, String) -> Unit)? = null

    private var productId: String = ""

    fun load(id: String) {
        productId = id
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getProductInfoById(id) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val parts = result.data.partIdsDesc.orEmpty()
                    if (parts.isEmpty()) {
                        showToast("抱歉,暂未该成品组件")
                    }
                    _ui.value = _ui.value.copy(oldParts = parts)
                }
            }
        }
    }

    fun addOldPart(item: ScenePartIdsDescDto) = addPart(item)

    fun addNewPart(item: ScenePartIdsDescDto) = addPart(item)

    fun removeNewPart(item: ScenePartIdsDescDto) {
        _ui.value = _ui.value.copy(newParts = _ui.value.newParts.filterNot { it.id == item.id })
    }

    fun next() {
        val newParts = _ui.value.newParts
        if (newParts.isEmpty()) {
            showToast("未查添加新组件")
            return
        }
        val defineIds = newParts.mapNotNull { it.partDefineInfoId }
        val partCodes = newParts.mapNotNull { it.code }
        viewModelScope.launch {
            showLoading()
            when (val define = withContext(ioDispatcher) {
                repository.getProductDefineByPartDefines(defineIds)
            }) {
                is ApiResult.Error -> {
                    hideLoading()
                    showToast(define.message)
                }
                is ApiResult.Success -> {
                    val defineId = define.data.id
                    if (defineId.isNullOrBlank()) {
                        hideLoading()
                        showToast("请求失败")
                        return@launch
                    }
                    when (val detail = withContext(ioDispatcher) {
                        repository.getProductDefineById(defineId, productId)
                    }) {
                        is ApiResult.Error -> {
                            hideLoading()
                            showToast(detail.message)
                        }
                        is ApiResult.Success -> {
                            hideLoading()
                            onOpenRegister?.invoke(detail.data, partCodes, productId)
                        }
                    }
                }
            }
        }
    }

    private fun addPart(item: ScenePartIdsDescDto) {
        if (_ui.value.newParts.any { it.id == item.id }) {
            showToast("请勿重复添加")
            return
        }
        _ui.value = _ui.value.copy(newParts = listOf(item) + _ui.value.newParts)
    }
}
