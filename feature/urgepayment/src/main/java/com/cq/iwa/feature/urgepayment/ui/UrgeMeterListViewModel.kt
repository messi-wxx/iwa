package com.cq.iwa.feature.urgepayment.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.urgepayment.data.UrgePaymentRepository
import com.cq.iwa.feature.urgepayment.network.UrgeMeterDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UrgeMeterListViewModel @Inject constructor(
    private val repository: UrgePaymentRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<UrgeMeterListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<UrgeMeterListUi>> = _uiState.asStateFlow()

    var onSearchClients: ((List<UrgeSearchItemUi>) -> Unit)? = null

    private var bookId: Int = 0
    private var taskName: String = ""
    private var source: List<UrgeMeterDto> = emptyList()
    private var showClientCode: Boolean = false
    private var loadJob: Job? = null

    fun load(bookId: Int, taskName: String, fromRefresh: Boolean = false) {
        this.bookId = bookId
        this.taskName = taskName
        if (bookId <= 0) {
            showToast("缺少参数")
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!fromRefresh) {
                _uiState.value = UiState.Loading
            }
            val displayClient = withContext(ioDispatcher) { repository.displayClientCode() }
            showClientCode = displayClient
            when (val result = withContext(ioDispatcher) { repository.getMetersForBook(bookId) }) {
                is ApiResult.Success -> {
                    source = result.data
                    publish()
                }
                is ApiResult.Error -> {
                    if (fromRefresh && _uiState.value is UiState.Success) {
                        showToast(result.message)
                        val current = (_uiState.value as UiState.Success).data
                        _uiState.value = UiState.Success(current.copy(tick = current.tick + 1))
                    } else {
                        _uiState.value = UiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun refresh() = load(bookId, taskName, fromRefresh = true)

    fun search(raw: String) {
        val keyword = raw.trim()
        if (keyword.isBlank()) {
            showToast("请输入查询关键字")
            return
        }
        if (source.isEmpty()) return
        val matched = source.asSequence()
            .filter { meter ->
                meter.clientCode.orEmpty().contains(keyword) ||
                    meter.clientName.orEmpty().contains(keyword) ||
                    meter.meterCode.orEmpty().contains(keyword)
            }
            .take(50)
            .map { meter ->
                UrgeSearchItemUi(
                    clientCode = meter.clientCode.orEmpty(),
                    title = listOf(meter.clientName.orEmpty(), meter.clientCode.orEmpty())
                        .filter { it.isNotBlank() }
                        .let { parts ->
                            when {
                                parts.size >= 2 -> "${parts[0]} （${parts[1]}）"
                                parts.isNotEmpty() -> parts.first()
                                else -> meter.meterCode.orEmpty()
                            }
                        },
                    address = meter.address.orEmpty(),
                )
            }
            .filter { it.clientCode.isNotBlank() }
            .toList()
        if (matched.isEmpty()) {
            showToast("未查到结果")
        } else {
            onSearchClients?.invoke(matched)
        }
    }

    private fun publish() {
        val meters = source.map { it.toUi() }
        val tick = (_uiState.value as? UiState.Success)?.data?.tick ?: 0
        _uiState.value = if (meters.isEmpty()) {
            if (_uiState.value is UiState.Empty) _uiState.value = UiState.Loading
            UiState.Empty
        } else {
            UiState.Success(UrgeMeterListUi(taskName = taskName, meters = meters, tick = tick + 1))
        }
    }

    private fun UrgeMeterDto.toUi(): UrgeMeterUi {
        val code = if (showClientCode) clientCode.orEmpty() else meterCode.orEmpty()
        val title = if (clientName.isNullOrBlank()) {
            code
        } else {
            "$clientName （$code）"
        }
        val count = totalFeeCount ?: 0
        val feeText = if (count == 0) {
            ""
        } else {
            "-${totalFee.orEmpty()}元(${count}笔)"
        }
        return UrgeMeterUi(
            clientCode = clientCode.orEmpty(),
            title = title,
            address = address.orEmpty(),
            feeText = feeText,
        )
    }
}
