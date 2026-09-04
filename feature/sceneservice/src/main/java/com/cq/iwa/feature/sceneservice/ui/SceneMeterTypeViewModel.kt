package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneDeviceInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneMeterTypeViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _deviceInfo = MutableStateFlow<SceneDeviceInfoDto?>(null)
    val deviceInfo: StateFlow<SceneDeviceInfoDto?> = _deviceInfo.asStateFlow()

    fun load(deviceId: Int) {
        if (deviceId == 0) return
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getDeviceInfo(deviceId.toLong()) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> _deviceInfo.value = result.data
            }
        }
    }
}
