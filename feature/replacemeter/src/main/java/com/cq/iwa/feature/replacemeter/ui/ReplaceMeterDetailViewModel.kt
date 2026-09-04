package com.cq.iwa.feature.replacemeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.replacemeter.ReplaceMeterState
import com.cq.iwa.feature.replacemeter.ReplaceShowWay
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import com.cq.iwa.feature.replacemeter.data.computeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReplaceMeterDetailViewModel @Inject constructor(
    private val repository: ReplaceMeterRepository,
    private val appSettings: AppSettings,
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<ReplaceMeterDetailUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<ReplaceMeterDetailUi>> = _uiState.asStateFlow()

    private var entity: ReplaceMeterEntity? = null
    private var needPosition = false
    private var chargeRead: String? = null

    fun load(tableId: Long) {
        launchUiState(_uiState) {
            val meter = repository.queryMeter(tableId) ?: error("未找到换表记录")
            entity = meter
            needPosition = repository.configYes("IsNeedPosition")
            chargeRead = repository.configValue("ChargeRead")
            meter.toUi()
        }
    }

    fun setShowWay(way: Int) {
        appSettings.replaceShowWay = way
        publish()
    }

    fun currentShowWay(): Int = appSettings.replaceShowWay

    fun updateDraft(
        oldReading: String? = null,
        newMeterCode: String? = null,
        newReading: String? = null,
        caliber: String? = null,
        verifyOrg: String? = null,
        verifyDate: String? = null,
        verifyExpireDate: String? = null,
        installType: String? = null,
        oldPhoto: String? = null,
        newPhoto: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ) {
        val meter = entity ?: return
        entity = meter.copy(
            oldReading = oldReading ?: meter.oldReading,
            newMeterCode = newMeterCode ?: meter.newMeterCode,
            newReading = newReading ?: meter.newReading,
            caliber = if (caliber != null) caliber.toIntOrNull() else meter.caliber,
            verifyOrg = verifyOrg ?: meter.verifyOrg,
            verifyDate = verifyDate ?: meter.verifyDate,
            verifyExpireDate = verifyExpireDate ?: meter.verifyExpireDate,
            installType = installType ?: meter.installType,
            oldPhotos = if (oldPhoto != null) {
                listOf(oldPhoto).filter { it.isNotBlank() }
            } else {
                meter.oldPhotos
            },
            newPhotos = if (newPhoto != null) {
                listOf(newPhoto).filter { it.isNotBlank() }
            } else {
                meter.newPhotos
            },
            latitude = latitude ?: meter.latitude,
            longitude = longitude ?: meter.longitude,
        )
        if (installType != null) appSettings.replaceInstallType = installType
        publish()
    }

    fun applyNfcCode(code: String) {
        if (!showNew()) return
        val cleaned = code.replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            .replace("zh", "", ignoreCase = false)
            .trim()
        if (cleaned.isBlank()) return
        updateDraft(newMeterCode = cleaned)
        publish()
    }

    suspend fun createPictureFile(): File = repository.createPictureFile()

    fun canAddEnvPhoto(): Boolean = (entity?.envPhotos?.size ?: 0) < 4

    fun addEnvPhoto(path: String) {
        val meter = entity ?: return
        if (meter.envPhotos.size >= 4) {
            showToast("环境图片最多上传四张")
            return
        }
        entity = meter.copy(envPhotos = meter.envPhotos + path)
        publish()
    }

    fun setOldPhoto(path: String) {
        updateDraft(oldPhoto = path)
        publish()
    }

    fun setNewPhoto(path: String) {
        updateDraft(newPhoto = path)
        publish()
    }

    fun removeEnvPhoto(index: Int) {
        val meter = entity ?: return
        entity = meter.copy(envPhotos = meter.envPhotos.filterIndexed { i, _ -> i != index })
        publish()
    }

    fun removeOldPhoto() {
        val meter = entity ?: return
        entity = meter.copy(oldPhotos = emptyList())
        publish()
    }

    fun removeNewPhoto() {
        val meter = entity ?: return
        entity = meter.copy(newPhotos = emptyList())
        publish()
    }

    fun validateAndSave(
        latitude: Double?,
        longitude: Double?,
        onNeedConfirm: (String) -> Unit,
        onSaved: (close: Boolean) -> Unit,
    ) {
        val meter = entity ?: return
        val showOld = showOld()
        val showNew = showNew()
        if (showOld) {
            if (meter.oldMeterCode.isNullOrBlank()) {
                showToast("无旧表表号")
                return
            }
            val oldReading = meter.oldReading
            if (oldReading.isNullOrBlank()) {
                showToast("未填写旧表止度")
                return
            }
            if (!isWaterRead(oldReading)) {
                showToast("水表读数最多九位整数")
                return
            }
            if (localPhoto(meter.oldPhotos) == null) {
                showToast("旧表未拍照")
                return
            }
        }
        if (showNew) {
            if (meter.newMeterCode.isNullOrBlank()) {
                showToast("无新表表号")
                return
            }
            val newReading = meter.newReading
            if (newReading.isNullOrBlank()) {
                showToast("未填写新表起度")
                return
            }
            if (!isWaterRead(newReading)) {
                showToast("水表读数最多九位整数")
                return
            }
            if (localPhoto(meter.newPhotos) == null) {
                showToast("新表未拍照")
                return
            }
            if (needPosition) {
                if (meter.envPhotos.none { it.isNotBlank() && it != "button" }) {
                    showToast("未添加标定环境图片")
                    return
                }
                if (latitude == null || longitude == null || (latitude == 0.0 && longitude == 0.0)) {
                    showToast("未获取到经纬度信息,请检查gps是否打开")
                    return
                }
            }
        }
        val oldDegree = meter.oldReading.orEmpty()
        val charge = meter.replaceRyFlux
        if (oldDegree.isNotBlank() && !charge.isNullOrBlank()) {
            val limit = chargeRead
            if (!limit.isNullOrBlank() && limit != "0") {
                val last = charge.toDoubleOrNull()
                val flux = limit.toDoubleOrNull()
                val old = oldDegree.toDoubleOrNull()
                if (last != null && flux != null && old != null && kotlin.math.abs(old - last) > flux) {
                    onNeedConfirm("旧表读数超过警戒值!")
                    return
                }
            }
        }
        persist(latitude, longitude, onSaved)
    }

    fun persist(latitude: Double?, longitude: Double?, onSaved: (close: Boolean) -> Unit) {
        viewModelScope.launch {
            val meter = entity ?: return@launch
            val newCode = meter.newMeterCode.orEmpty()
            val duplicate = if (newCode.isBlank()) {
                false
            } else {
                withContext(ioDispatcher) {
                    repository.hasDuplicateNewCode(meter.taskId, newCode, meter.tableId)
                }
            }
            if (duplicate) {
                showToast("新表表号已存在")
                return@launch
            }
            val locPending = showNew() && meter.envPhotos.any { it.isNotBlank() && it != "button" }
            val saved = meter.copy(
                state = ReplaceMeterState.PENDING,
                progress = computeProgress(meter.oldReading, meter.newMeterCode),
                latitude = if (locPending) latitude ?: 0.0 else meter.latitude,
                longitude = if (locPending) longitude ?: 0.0 else meter.longitude,
                locationPending = locPending,
                installType = meter.installType ?: appSettings.replaceInstallType,
            )
            withContext(ioDispatcher) {
                repository.saveMeter(saved)
            }
            entity = saved
            showToast("成功")
            val forward = withContext(ioDispatcher) { authRepository.isAutoNextEnabled() }
            val next = withContext(ioDispatcher) {
                if (forward) {
                    repository.queryNext(
                        taskId = saved.taskId,
                        sort = saved.sort,
                        tableId = saved.tableId,
                        groupName = appSettings.replaceGroupName,
                    )
                } else {
                    repository.queryPrevious(
                        taskId = saved.taskId,
                        sort = saved.sort,
                        tableId = saved.tableId,
                        groupName = appSettings.replaceGroupName,
                    )
                }
            }
            if (next == null) {
                showToast(if (forward) "该任务最后一块表了" else "该任务第一块表了")
                onSaved(true)
            } else {
                entity = next
                publish()
                showToast(if (forward) "已跳到下一块表" else "已跳到上一块表")
                onSaved(false)
            }
        }
    }

    private fun publish() {
        val meter = entity ?: return
        _uiState.value = UiState.Success(meter.toUi())
    }

    private fun showOld(): Boolean {
        val way = appSettings.replaceShowWay
        return way == ReplaceShowWay.OLD_ONLY || way == ReplaceShowWay.BOTH
    }

    private fun showNew(): Boolean {
        val way = appSettings.replaceShowWay
        return way == ReplaceShowWay.NEW_ONLY || way == ReplaceShowWay.BOTH
    }

    private fun ReplaceMeterEntity.toUi(): ReplaceMeterDetailUi {
        val install = installType?.takeIf { it.isNotBlank() } ?: appSettings.replaceInstallType
        return ReplaceMeterDetailUi(
            tableId = tableId,
            taskId = taskId,
            address = "${oldMeterCode.orEmpty()}---${this.address.orEmpty()}",
            placeAddress = this.address.orEmpty(),
            clientCode = clientCode.orEmpty(),
            oldMeterCode = oldMeterCode.orEmpty(),
            oldReading = oldReading.orEmpty(),
            replaceRyFlux = replaceRyFlux.orEmpty(),
            newMeterCode = newMeterCode.orEmpty(),
            newReading = newReading?.ifBlank { "0" } ?: "0",
            caliber = caliber?.toString().orEmpty(),
            oldPhoto = localPhoto(oldPhotos).orEmpty(),
            newPhoto = localPhoto(newPhotos).orEmpty(),
            envPhotos = envPhotos.filter { it.isNotBlank() && it != "button" },
            verifyOrg = verifyOrg.orEmpty(),
            verifyDate = verifyDate.orEmpty(),
            verifyExpireDate = verifyExpireDate.orEmpty(),
            installType = install,
            showOld = showOld(),
            showNew = showNew(),
            needPosition = needPosition,
        )
    }

    private fun localPhoto(list: List<String>): String? =
        list.firstOrNull { it.isNotBlank() && it != "button" }
}

fun isWaterRead(read: String): Boolean {
    if (read == "0") return true
    return Regex("^[1-9]\\d{0,8}$").matches(read)
}
