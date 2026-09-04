package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneReplacePartBodyDto
import com.cq.iwa.feature.sceneservice.network.SceneReplaceProductInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneProductRegisterViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var onReplaced: (() -> Unit)? = null

    fun submit(
        productId: String,
        productDefineId: String?,
        productNumber: String,
        fullCode: String,
        partCodes: List<String>,
        properties: Map<String, String>,
    ) {
        if (productNumber.isBlank()) {
            showToast("未填写成品编号")
            return
        }
        if (fullCode.isBlank()) {
            showToast("未填写识别码")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.replacePart(
                    SceneReplacePartBodyDto(
                        productId = productId,
                        productInfo = SceneReplaceProductInfoDto(
                            code = productNumber,
                            fullCode = fullCode,
                            productDefineId = productDefineId,
                            propertysList = properties.takeIf { it.isNotEmpty() },
                            partCodes = partCodes,
                        ),
                    ),
                )
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("更换成功")
                    onReplaced?.invoke()
                }
            }
        }
    }
}
