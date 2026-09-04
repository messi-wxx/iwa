package com.cq.iwa.feature.urgepayment.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.urgepayment.data.UrgePaymentRepository
import com.cq.iwa.feature.urgepayment.network.UrgeDeviceDto
import com.cq.iwa.feature.urgepayment.network.UrgeFeeClientDto
import com.cq.iwa.feature.urgepayment.network.UrgeFeeDetailDto
import com.cq.iwa.feature.urgepayment.network.UrgeReadInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class UrgePaymentDetailViewModel @Inject constructor(
    private val repository: UrgePaymentRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<UrgeDetailUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<UrgeDetailUi>> = _uiState.asStateFlow()

    var onReadHistory: ((List<UrgeReadHistoryUi>) -> Unit)? = null

    private var clientCode: String = ""
    private var client: UrgeFeeClientDto? = null
    private var fees: List<UrgeFeeDetailDto> = emptyList()

    fun load(code: String) {
        if (code.isBlank()) return
        clientCode = code
        viewModelScope.launch {
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            } else {
                showLoading()
            }
            when (val result = withContext(ioDispatcher) { repository.getDetailByCode(code) }) {
                is ApiResult.Error -> {
                    hideLoading()
                    if (_uiState.value is UiState.Success) {
                        showToast(result.message)
                    } else {
                        _uiState.value = UiState.Error(result.message)
                    }
                }
                is ApiResult.Success -> {
                    client = result.data
                    loadFees(result.data.clientId)
                }
            }
        }
    }

    fun refresh() {
        if (clientCode.isBlank()) return
        load(clientCode)
    }

    fun previous() = jump(previous = true)

    fun next() = jump(previous = false)

    fun changePhone(phone: String) {
        if (clientCode.isBlank()) {
            showToast("无法操作,客户号为空！")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.changePhone(clientCode, phone) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val info = client?.clientInfo
                    client = client?.copy(clientInfo = info?.copy(cellPhone = phone))
                    publish()
                    showToast("修改成功")
                }
            }
        }
    }

    fun changeRemark(deviceId: Int, remark: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.changeRemark(deviceId, remark) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val devices = client?.devices.orEmpty().map { device ->
                        if (device.deviceId == deviceId) device.copy(readerRemark = remark) else device
                    }
                    client = client?.copy(devices = devices)
                    publish()
                    showToast("修改成功")
                }
            }
        }
    }

    fun openValve(deviceId: Int) = switchValve(deviceId, open = true)

    fun closeValve(deviceId: Int) = switchValve(deviceId, open = false)

    fun loadReadHistory(deviceId: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getReadHistory(deviceId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val records = result.data.value
                    if (records.isEmpty()) {
                        showToast("未查到抄表记录")
                    } else {
                        onReadHistory?.invoke(records.map { it.toUi() })
                    }
                }
            }
        }
    }

    private fun jump(previous: Boolean) {
        val id = client?.clientId ?: return
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (previous) repository.previousClient(id) else repository.nextClient(id)
            }
            when (result) {
                is ApiResult.Error -> {
                    hideLoading()
                    showToast(result.message)
                }
                is ApiResult.Success -> {
                    val code = result.data.code.orEmpty()
                    if (code.isBlank()) {
                        hideLoading()
                        showToast("查询失败")
                    } else {
                        load(code)
                    }
                }
            }
        }
    }

    private fun switchValve(deviceId: Int, open: Boolean) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                if (open) repository.openValve(deviceId) else repository.closeValve(deviceId)
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> showToast(if (open) "开阀成功" else "关阀成功")
            }
        }
    }

    private suspend fun loadFees(clientId: Int) {
        when (val result = withContext(ioDispatcher) { repository.getNopayFees(clientId) }) {
            is ApiResult.Error -> {
                hideLoading()
                fees = emptyList()
                publish()
                showToast(result.message)
            }
            is ApiResult.Success -> {
                hideLoading()
                fees = result.data
                publish()
            }
        }
    }

    private fun publish() {
        val data = client ?: return
        val info = data.clientInfo
        _uiState.value = UiState.Success(
            UrgeDetailUi(
                clientCode = clientCode,
                clientId = data.clientId,
                name = info?.name.orEmpty(),
                code = info?.code.orEmpty(),
                phone = info?.cellPhone.orEmpty(),
                address = info?.address.orEmpty(),
                state = info?.stateDesc.orEmpty(),
                openDate = formatDate(info?.openDate),
                remark = info?.remark.orEmpty(),
                balance = data.balance.orEmpty(),
                oweFee = oweFeeText(fees),
                hasFees = fees.isNotEmpty(),
                fees = fees.map { it.toUi() },
                hasDevices = data.devices.isNotEmpty(),
                devices = data.devices.map { it.toUi() },
            ),
        )
    }

    private fun oweFeeText(items: List<UrgeFeeDetailDto>): String {
        if (items.isEmpty()) return "无欠费"
        var sum = BigDecimal.ZERO
        items.forEach { item ->
            val raw = item.actualFee?.trim().orEmpty()
            if (raw.isNotBlank()) {
                sum = runCatching { sum.add(BigDecimal(raw)) }.getOrDefault(sum)
            }
        }
        return "${sum.toPlainString()}元"
    }

    private fun UrgeFeeDetailDto.toUi(): UrgeFeeItemUi {
        val late = lateFee ?: 0f
        val lateText = if (late > 0) {
            "$late(${lateDays ?: 0}天)"
        } else {
            "0"
        }
        return UrgeFeeItemUi(
            month = theMonth.orEmpty(),
            meter = deviceAndSystemModuleName.orEmpty(),
            startQty = beginQtyStr.orEmpty(),
            endQty = endQtyStr.orEmpty(),
            useQty = normalQtyStr.orEmpty(),
            receivableFee = receivableFee?.toString().orEmpty(),
            lateFee = lateText,
        )
    }

    private fun UrgeDeviceDto.toUi(): UrgeDeviceItemUi =
        UrgeDeviceItemUi(
            deviceId = deviceId,
            meterCode = deviceCode.orEmpty(),
            caliber = caliberDesc.orEmpty(),
            feeKind = deviceFeeKindNameDesc.orEmpty(),
            feeState = deviceFeeStateDesc.orEmpty(),
            chargeWay = chargeWayDesc.orEmpty(),
            reading = lastEndQty.orEmpty(),
            readDate = formatDate(lastReadDate),
            valveState = valveStateDesc.orEmpty(),
            useState = deviceUsageStateDesc.orEmpty(),
            bookName = fullBookName.orEmpty(),
            remark = readerRemark.orEmpty(),
        )

    private fun UrgeReadInfoDto.toUi(): UrgeReadHistoryUi =
        UrgeReadHistoryUi(
            meterCode = meterCode.orEmpty(),
            readDate = formatReadTime(readTime),
            reading = reading.orEmpty(),
            source = source.orEmpty(),
            readUser = readUserName.orEmpty(),
            auditUser = auditUserName.orEmpty(),
        )

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return if (raw.contains("T")) raw.substringBefore("T") else raw
    }

    private fun formatReadTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val normalized = raw.replace("T", " ")
        return if (normalized.length >= 16) normalized.substring(0, 16) else normalized
    }
}
