package com.cq.iwa.core.ui.ext

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cq.iwa.core.common.model.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun <T> LifecycleOwner.collectUiState(
    stateFlow: StateFlow<UiState<T>>,
    onLoading: () -> Unit = {},
    onSuccess: (T) -> Unit,
    onError: (String) -> Unit = {},
    onEmpty: () -> Unit = {},
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            stateFlow.collect { state ->
                when (state) {
                    is UiState.Idle -> Unit
                    is UiState.Loading -> onLoading()
                    is UiState.Success -> onSuccess(state.data)
                    is UiState.Error -> onError(state.message)
                    is UiState.Empty -> onEmpty()
                }
            }
        }
    }
}

fun <T> Fragment.collectUiState(
    stateFlow: StateFlow<UiState<T>>,
    onLoading: () -> Unit = {},
    onSuccess: (T) -> Unit,
    onError: (String) -> Unit = {},
    onEmpty: () -> Unit = {},
) {
    viewLifecycleOwner.collectUiState(
        stateFlow = stateFlow,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
        onEmpty = onEmpty,
    )
}
