package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.ScenePartIdsDescDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneInputComponentViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var onFound: ((ScenePartIdsDescDto) -> Unit)? = null

    fun query(code: String) {
        if (code.isBlank()) {
            showToast("未填写组件编号或识别码")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getPartInfoByCode(code.trim()) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val define = result.data.partDefineIdDesc
                    if (define == null) {
                        showToast("未查到该组件")
                    } else {
                        onFound?.invoke(
                            ScenePartIdsDescDto(
                                id = result.data.id,
                                partDefineInfoId = define.id,
                                partDefineInfoCode = define.code,
                                partDefineInfoName = define.name,
                                code = result.data.code,
                            ),
                        )
                    }
                }
            }
        }
    }
}
