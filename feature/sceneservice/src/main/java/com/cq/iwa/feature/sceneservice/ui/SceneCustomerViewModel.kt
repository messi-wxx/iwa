package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneCustomerDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SceneCustomerViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<List<SceneCustomerDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<SceneCustomerDto>>> = _uiState.asStateFlow()

    fun query(name: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = withContext(ioDispatcher) { repository.queryCustomers(name) }) {
                is ApiResult.Error -> _uiState.value = UiState.Error(result.message)
                is ApiResult.Success -> {
                    val list = result.data.toMutableList()
                    if (list.isEmpty()) {
                        showToast("未查到水司，请重试！")
                        _uiState.value = UiState.Empty
                    } else {
                        list.add(
                            0,
                            SceneCustomerDto(text = "全部水司(耗时较长)", value = ""),
                        )
                        _uiState.value = UiState.Success(list)
                    }
                }
            }
        }
    }
}
