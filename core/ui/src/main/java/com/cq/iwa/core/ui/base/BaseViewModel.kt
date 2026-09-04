package com.cq.iwa.core.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.exception.AppException
import com.cq.iwa.core.common.exception.toAppException
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiEvent
import com.cq.iwa.core.common.model.UiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel(
    protected open val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    protected fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    protected fun <T> mutableUiState(initial: UiState<T> = UiState.Idle): MutableStateFlow<UiState<T>> =
        MutableStateFlow(initial)

    protected fun <T> StateFlow<UiState<T>>.asUiState(): StateFlow<UiState<T>> = this

    protected fun showToast(message: String) {
        sendEvent(UiEvent.Toast(message))
    }

    protected fun showToast(@StringRes messageRes: Int) {
        sendEvent(UiEvent.ToastRes(messageRes))
    }

    protected fun showLoading() {
        sendEvent(UiEvent.ShowLoading)
    }

    protected fun hideLoading() {
        sendEvent(UiEvent.HideLoading)
    }

    /**
     * 在 IO 线程执行网络/数据库操作，自动映射为 [ApiResult]
     */
    protected fun <T> launchApi(
        block: suspend () -> T,
        onResult: (ApiResult<T>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) { block() }
            }.fold(
                onSuccess = { ApiResult.Success(it) },
                onFailure = { it.toApiError() },
            )
            onResult(result)
        }
    }

    /**
     * 带 UiState 的协程启动器。集合为空时进入 [UiState.Empty]。
     */
    protected fun <T> launchUiState(
        stateFlow: MutableStateFlow<UiState<T>>,
        isEmpty: ((T) -> Boolean)? = null,
        block: suspend () -> T,
    ) {
        viewModelScope.launch {
            stateFlow.value = UiState.Loading
            val result = runCatching {
                withContext(ioDispatcher) { block() }
            }
            stateFlow.value = result.fold(
                onSuccess = { data -> data.toUiState(isEmpty) },
                onFailure = {
                    val appEx = it.toAppException()
                    UiState.Error(appEx.message ?: "请求失败", it)
                },
            )
        }
    }

    private fun <T> T.toUiState(isEmpty: ((T) -> Boolean)?): UiState<T> {
        val empty = isEmpty?.invoke(this) ?: (this is Collection<*> && this.isEmpty())
        return if (empty) UiState.Empty else UiState.Success(this)
    }

    private fun Throwable.toApiError(): ApiResult.Error {
        val appEx = toAppException()
        val code = (appEx as? AppException.ServerException)?.code ?: -1
        return ApiResult.Error(code, appEx.message ?: "请求失败", this)
    }
}
