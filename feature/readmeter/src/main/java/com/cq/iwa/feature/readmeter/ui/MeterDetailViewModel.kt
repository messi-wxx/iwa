package com.cq.iwa.feature.readmeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.core.media.PhotoProcessor
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.data.BcpArchiveRepository
import com.cq.iwa.feature.readmeter.data.MeterLocalStore
import com.cq.iwa.feature.readmeter.data.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MeterDetailViewModel @Inject constructor(
    private val repository: MeterRepository,
    private val bcpArchiveRepository: BcpArchiveRepository,
    private val localStore: MeterLocalStore,
    private val appSettings: AppSettings,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<MeterDetailUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<MeterDetailUi>> = _uiState.asStateFlow()

    var remarkOptions: List<String> = emptyList()
        private set

    private var entity: ReadMeterEntity? = null
    private var autoNextForward = true
    private var forceNfc = false
    private var calculateUsage = false
    private var nfcUnlocked = false
    private var requirePhoto = false
    private var integerReading = false
    private var showEnvironmentView = false
    private var accountCustomerCode = ""
    private var accountUserCode = ""

    fun load(tableId: Long, fromNfc: Boolean) {
        nfcUnlocked = fromNfc
        launchUiState(_uiState) {
            val user = repository.currentUser()
            accountCustomerCode = user.customerCode
            accountUserCode = user.code
            remarkOptions = repository.remarks()
            autoNextForward = repository.configYes("autoNext")
            forceNfc = repository.configYes("forceNfcReading")
            calculateUsage = repository.configYes("calculateReadingQty")
            requirePhoto = repository.configYes("IsTakePic")
            val platform = repository.platform()
            integerReading = platform != MeterPlatform.BCP
            showEnvironmentView = integerReading && repository.configYes("IsShowEnvironmentView")
            val meter = repository.queryMeter(tableId) ?: error("未找到水表")
            entity = meter
            meter.toUi()
        }
    }

    fun onNfcTag(meterCode: String, onJump: (Long) -> Unit) {
        val code = meterCode.trim()
        if (unlockByNfc(code)) {
            showToast("刷卡成功")
            return
        }
        viewModelScope.launch {
            val meter = withContext(ioDispatcher) { repository.queryByMeterCode(code) }
            if (meter == null) {
                showToast("未找到该水表")
            } else {
                onJump(meter.tableId)
            }
        }
    }

    fun canAddPhoto(env: Boolean): Boolean {
        val meter = entity ?: return false
        return if (env) meter.envPhotos.size < 4 else meter.photos.size < 3
    }

    fun createPictureFile(): File {
        val customerCode = entity?.customerCode?.ifBlank { accountCustomerCode } ?: accountCustomerCode
        val userCode = entity?.userCode?.ifBlank { accountUserCode } ?: accountUserCode
        return localStore.createPictureFile(customerCode, userCode)
    }

    fun unlockByNfc(meterCode: String): Boolean {
        val current = entity ?: return false
        val match = current.meterCode.equals(meterCode, ignoreCase = true)
        if (match) {
            nfcUnlocked = true
            publish()
        }
        return match
    }

    fun updateReading(reading: String) {
        val meter = entity ?: return
        entity = meter.copy(reading = reading)
        publish()
    }

    fun updateRemark(remark: String) {
        val meter = entity ?: return
        entity = meter.copy(remark = remark)
        publish()
    }

    suspend fun addCapturedPhoto(file: File, env: Boolean = false): Boolean {
        val ok = PhotoProcessor.compressLocalFile(file.absolutePath)
        if (!ok) {
            showToast("图片压缩失败")
            file.delete()
            return false
        }
        addPhoto(file.absolutePath, env)
        return true
    }

    fun addPhoto(path: String, env: Boolean = false) {
        val meter = entity ?: return
        entity = if (env) {
            if (meter.envPhotos.size >= 4) {
                showToast("环境照片最多4张")
                return
            }
            meter.copy(envPhotos = meter.envPhotos + path)
        } else {
            if (meter.photos.size >= 3) {
                showToast("最多只能上传三张图片")
                return
            }
            meter.copy(photos = meter.photos + path)
        }
        publish()
    }

    fun removePhoto(index: Int, env: Boolean = false) {
        val meter = entity ?: return
        entity = if (env) {
            meter.copy(envPhotos = meter.envPhotos.filterIndexed { i, _ -> i != index })
        } else {
            meter.copy(photos = meter.photos.filterIndexed { i, _ -> i != index })
        }
        publish()
    }

    fun usageText(): String {
        val last = entity?.extInfo?.get("上期抄读止度")?.toDoubleOrNull()
            ?: entity?.lastRead?.toDoubleOrNull()
        val readingRaw = entity?.reading.orEmpty()
        if (last == null || readingRaw.isBlank()) return ""
        if (integerReading) {
            if (!isIntegerReading(readingRaw)) return ""
        } else if (!calculateUsage) {
            return ""
        }
        val current = readingRaw.toDoubleOrNull() ?: return ""
        return (current - last).toInt().toString()
    }

    fun validateAndSave(
        latitude: Double? = null,
        longitude: Double? = null,
        onNeedConfirm: (String) -> Unit,
        onSaved: () -> Unit,
    ) {
        val meter = entity ?: return
        if (forceNfc && !nfcUnlocked) {
            showToast("请先刷卡后再保存")
            return
        }
        val remark = meter.remark.orEmpty().ifBlank { "正常抄表" }
        viewModelScope.launch {
            if (remark == "正常抄表") {
                val reading = meter.reading.orEmpty()
                if (reading.isBlank()) {
                    showToast("未填写表度数")
                    return@launch
                }
                if (integerReading) {
                    if (!isIntegerReading(reading)) {
                        showToast("水表读数最多九位整数")
                        return@launch
                    }
                } else if (!reading.matches(Regex("^[0-9]{1,9}(\\.[0-9]{1,3})?$"))) {
                    showToast("水表读数最多九位整数和三位小数")
                    return@launch
                }
                if (requirePhoto && meter.photos.isEmpty()) {
                    showToast("未拍照")
                    return@launch
                }
                val tip = withContext(ioDispatcher) { warningTip(meter.copy(remark = remark)) }
                if (!tip.isNullOrBlank()) {
                    onNeedConfirm(tip)
                    return@launch
                }
            } else if (remark == "其它") {
                showToast("请填写无法抄表具体原因")
                return@launch
            } else if (remark.isBlank()) {
                showToast("请选择或填写备注")
                return@launch
            }
            persist(latitude, longitude, onSaved)
        }
    }

    fun persist(
        latitude: Double? = null,
        longitude: Double? = null,
        onSaved: () -> Unit,
    ) {
        val meter = entity ?: return
        viewModelScope.launch {
            val env = meter.envPhotos.filter { it.isNotBlank() && it != "button" }
            val toSave = meter.copy(
                remark = meter.remark.orEmpty().ifBlank { "正常抄表" },
                latitude = if (env.isNotEmpty()) latitude ?: 0.0 else meter.latitude,
                longitude = if (env.isNotEmpty()) longitude ?: 0.0 else meter.longitude,
            )
            val saved = withContext(ioDispatcher) {
                repository.saveReading(toSave)
            }
            entity = saved
            showToast("保存成功")
            if (!forceNfc) {
                jump(autoNextForward)
            } else {
                publish()
            }
            onSaved()
        }
    }

    fun jump(forward: Boolean) {
        val meter = entity ?: return
        viewModelScope.launch {
            val next = withContext(ioDispatcher) {
                repository.nextUnread(meter, forward, appSettings.readMeterGroupName)
            }
            if (next == null) {
                showToast(if (forward) "已经是最后一个未抄水表了" else "已经是第一个未抄水表了")
            } else {
                nfcUnlocked = false
                entity = next.copy(remark = next.remark?.ifBlank { "正常抄表" } ?: "正常抄表")
                publish()
            }
        }
    }

    fun jumpLastRead() {
        val meter = entity ?: return
        viewModelScope.launch {
            val last = withContext(ioDispatcher) {
                repository.lastRead(meter.taskId, meter.readTime, appSettings.readMeterGroupName)
            }
            if (last == null) {
                showToast("没有发现最近有抄表")
            } else {
                entity = last
                nfcUnlocked = !forceNfc
                publish()
                showToast("已返回到最近的抄表")
            }
        }
    }

    fun changePhone(phone: String) {
        val meter = entity ?: return
        if (meter.clientCode.isNullOrBlank()) {
            showToast("户号为空，无法改电话")
            return
        }
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    bcpArchiveRepository.changePhone(meter.clientCode.orEmpty(), meter.tableId, phone)
                }
            ) {
                is ApiResult.Success -> {
                    showToast("修改成功")
                    reload()
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }

    fun changeDescribe(describe: String) {
        val meter = entity ?: return
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    bcpArchiveRepository.changeDescribe(meter.meterId, meter.tableId, describe)
                }
            ) {
                is ApiResult.Success -> {
                    showToast("修改成功")
                    reload()
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }

    private fun reload() {
        val id = entity?.tableId ?: return
        load(id, nfcUnlocked)
    }

    private fun publish() {
        val meter = entity ?: return
        _uiState.value = UiState.Success(meter.toUi())
    }

    private fun ReadMeterEntity.toUi(): MeterDetailUi {
        val ext = extInfo.entries.map { it.key to (it.value.orEmpty()) }
        val phone = extInfo["联系电话"].orEmpty().ifBlank { cellPhone.orEmpty() }
        return MeterDetailUi(
            tableId = tableId,
            meterId = meterId,
            taskId = taskId,
            meterCode = meterCode.orEmpty(),
            caliber = caliber.orEmpty(),
            address = address.orEmpty(),
            clientName = clientName.orEmpty(),
            clientCode = clientCode.orEmpty(),
            reading = reading.orEmpty(),
            remark = remark?.ifBlank { "正常抄表" } ?: "正常抄表",
            lastRead = lastRead.orEmpty(),
            photos = photos,
            envPhotos = envPhotos,
            extInfo = ext,
            moreInfo = ext.joinToString("\n") { (key, value) ->
                "$key：${MeterExtInfoDisplay.displayValue(key, value)}"
            },
            hasExtInfo = ext.isNotEmpty(),
            phone = phone,
            usageText = usageText(),
            forceNfc = forceNfc,
            calculateUsage = calculateUsage,
            showEnvironmentView = showEnvironmentView,
            nfcUnlocked = nfcUnlocked || !forceNfc,
        )
    }

    private fun isIntegerReading(read: String): Boolean {
        if (read == "0") return true
        return read.matches(Regex("^[1-9]\\d*$"))
    }

    private suspend fun warningTip(meter: ReadMeterEntity): String? {
        val last = meter.extInfo["上期抄读止度"]?.toDoubleOrNull()
            ?: meter.lastRead?.toDoubleOrNull()
        val current = meter.reading?.toDoubleOrNull() ?: return null
        var usage = 0
        if (last != null) {
            usage = (current - last).toInt()
            if (usage < 0) return "小于上期读数"
            if (usage == 0) return null
        }
        val min = repository.configValue("minAuditWater")?.toIntOrNull()
        val max = repository.configValue("maxAuditWater")?.toIntOrNull()
        val averagePercent = repository.configValue("compareAverageWater")?.toFloatOrNull()
        val average = meter.extInfo["平均用量"]?.toDoubleOrNull()
        if (min != null && usage < min) return null
        if (max != null && usage > max) return "用量已超过设定的最大用量${max}m³"
        if (average != null && average > 0 && averagePercent != null) {
            val limit = (average * averagePercent).toInt()
            if (usage > limit) return "用量超过平均用量设定的比例"
        }
        return null
    }
}
