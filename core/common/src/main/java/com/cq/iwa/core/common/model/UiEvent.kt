package com.cq.iwa.core.common.model

import androidx.annotation.StringRes

/**
 * 一次性事件（Toast、导航、弹窗、Loading），避免配置变更重复消费
 */
sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
    data class ToastRes(@StringRes val messageRes: Int) : UiEvent
    data class Navigate(val route: String) : UiEvent
    data class ShowMessage(val title: String, val message: String) : UiEvent
    data object ShowLoading : UiEvent
    data object HideLoading : UiEvent
}
