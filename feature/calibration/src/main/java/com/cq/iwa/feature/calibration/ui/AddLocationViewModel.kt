package com.cq.iwa.feature.calibration.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.calibration.data.CalibrationRepository
import com.cq.iwa.feature.calibration.network.LocationPlaceSubmitDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddLocationViewModel @Inject constructor(
    private val repository: CalibrationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val photos = mutableListOf<String>()

    fun photos(): List<String> = photos.toList()

    fun addPhoto(path: String) {
        photos += path
    }

    fun removePhoto(index: Int) {
        if (index in photos.indices) photos.removeAt(index)
    }

    fun hasDraft(remark: String): Boolean = photos.isNotEmpty() || remark.isNotBlank()

    suspend fun createPictureFile(): File = repository.createPictureFile()

    fun submit(
        isMeter: Boolean,
        linkId: Long,
        meterCode: String?,
        remark: String,
        lat: Double?,
        lng: Double?,
        onDone: () -> Unit,
    ) {
        if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
            showToast("未在地图标记位置")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.submitPlace(
                    isMeter = isMeter,
                    body = LocationPlaceSubmitDto(
                        linkId = linkId,
                        meterCode = meterCode?.takeIf { isMeter && it.isNotBlank() },
                        lat = lat,
                        lng = lng,
                        remark = remark,
                    ),
                    photoPaths = photos.toList(),
                    meterCode = meterCode,
                )
            }
            hideLoading()
            when (result) {
                is ApiResult.Success -> {
                    showToast("提交成功")
                    onDone()
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }
}
