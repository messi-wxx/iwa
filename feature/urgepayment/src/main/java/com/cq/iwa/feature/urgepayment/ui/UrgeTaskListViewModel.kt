package com.cq.iwa.feature.urgepayment.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.urgepayment.data.UrgePaymentRepository
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
class UrgeTaskListViewModel @Inject constructor(
    private val repository: UrgePaymentRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<UrgeTaskListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<UrgeTaskListUi>> = _uiState.asStateFlow()

    var onSearchClients: ((List<UrgeSearchItemUi>) -> Unit)? = null
    private var loadJob: Job? = null

    fun load(fromRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!fromRefresh) {
                _uiState.value = UiState.Loading
            }
            when (val result = withContext(ioDispatcher) { repository.getUrgeTasks() }) {
                is ApiResult.Success -> {
                    val tasks = result.data.map { dto ->
                        val count = dto.totalFeeCount ?: 0
                        UrgeTaskUi(
                            bookId = dto.id,
                            taskName = dto.taskName.orEmpty().ifBlank { "催费任务" },
                            feeCountText = if (count == 0) "" else "(${count}笔)",
                        )
                    }
                    val tick = (_uiState.value as? UiState.Success)?.data?.tick ?: 0
                    _uiState.value = if (tasks.isEmpty()) {
                        if (_uiState.value is UiState.Empty) _uiState.value = UiState.Loading
                        UiState.Empty
                    } else {
                        UiState.Success(UrgeTaskListUi(tasks, tick + 1))
                    }
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

    fun search(raw: String) {
        val keyword = raw.trim()
        if (keyword.isBlank()) {
            showToast("请输入查询关键字")
            return
        }
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.searchClients(keyword) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val items = result.data.value.map { client ->
                        UrgeSearchItemUi(
                            clientCode = client.code.orEmpty(),
                            title = listOf(client.name.orEmpty(), client.code.orEmpty())
                                .filter { it.isNotBlank() }
                                .let { parts ->
                                    when {
                                        parts.size >= 2 -> "${parts[0]} （${parts[1]}）"
                                        parts.isNotEmpty() -> parts.first()
                                        else -> "未知用户"
                                    }
                                },
                            address = client.address.orEmpty(),
                        )
                    }.filter { it.clientCode.isNotBlank() }
                    if (items.isEmpty()) {
                        showToast("未查到符合条件的水表")
                    } else {
                        onSearchClients?.invoke(items)
                    }
                }
            }
        }
    }
}
