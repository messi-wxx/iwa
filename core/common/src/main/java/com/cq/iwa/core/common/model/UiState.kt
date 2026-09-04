package com.cq.iwa.core.common.model

/**
 * 统一页面 UI 状态
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
