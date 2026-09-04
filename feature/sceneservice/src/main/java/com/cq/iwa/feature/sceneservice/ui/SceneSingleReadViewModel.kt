package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneDeviceInfoDto
import com.cq.iwa.feature.sceneservice.network.SceneSingleReadRequestDto
import com.cq.iwa.feature.sceneservice.network.SceneWaterRealtimeViewModelsDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneSingleReadViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var onReplaceContinue: ((SceneDeviceInfoDto) -> Unit)? = null

    fun save(
        deviceId: Int,
        deviceInfo: SceneDeviceInfoDto?,
        reading: String,
        displayFlux: String,
        positiveFlux: String,
        inversionFlux: String,
    ) {
        if (reading.isBlank()) {
            showToast("未填写表度数")
            return
        }
        if (!isSceneWaterRead(reading)) {
            showToast("水表读数最多九位整数")
            return
        }
        if (displayFlux.isNotBlank() && !isSceneWaterRead(displayFlux)) {
            showToast("累计流量最多九位整数")
            return
        }
        if (positiveFlux.isNotBlank() && !isSceneWaterRead(positiveFlux)) {
            showToast("正向流量最多九位整数")
            return
        }
        if (inversionFlux.isNotBlank() && !isSceneWaterRead(inversionFlux)) {
            showToast("反向流量最多九位整数")
            return
        }
        val water = reading.toFloat()
        val realtime = SceneWaterRealtimeViewModelsDto(
            water = water,
            displayFlux = displayFlux.toFloatOrNull() ?: 0f,
            positiveFlux = positiveFlux.toFloatOrNull() ?: 0f,
            inversionFlux = inversionFlux.toFloatOrNull() ?: 0f,
        )
        if (deviceId != 0 && deviceInfo == null) {
            viewModelScope.launch {
                showLoading()
                val result = withContext(ioDispatcher) {
                    repository.singleReadSave(
                        SceneSingleReadRequestDto(
                            id = deviceId,
                            water = water,
                            displayFlux = displayFlux.toFloatOrNull(),
                            positiveFlux = positiveFlux.toFloatOrNull(),
                            inversionFlux = inversionFlux.toFloatOrNull(),
                        ),
                    )
                }
                hideLoading()
                when (result) {
                    is ApiResult.Error -> showToast(result.message)
                    is ApiResult.Success -> showToast("保存成功")
                }
            }
        } else if (deviceId == 0 && deviceInfo != null) {
            onReplaceContinue?.invoke(deviceInfo.copy(waterRealtimeViewModels = realtime))
        } else {
            showToast("无法继续,设备不存在")
        }
    }
}
