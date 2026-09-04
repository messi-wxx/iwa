package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneICodeViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var onUpdated: (() -> Unit)? = null

    fun submit(flag: Int, deviceId: String?, fullCode: String) {
        if (deviceId.isNullOrBlank()) {
            showToast("设备id为空")
            return
        }
        if (fullCode.isBlank()) {
            showToast("请输入新的识别码")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (flag == 1) {
                    repository.updateProductICode(deviceId, fullCode)
                } else {
                    repository.updatePartICode(deviceId, fullCode)
                }
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("更新成功")
                    onUpdated?.invoke()
                }
            }
        }
    }
}
