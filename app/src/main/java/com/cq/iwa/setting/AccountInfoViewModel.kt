package com.cq.iwa.setting

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AccountInfoUiModel(
    val userName: String,
    val userCode: String,
    val customerCode: String,
    val customerName: String,
)

@HiltViewModel
class AccountInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<AccountInfoUiModel>>(UiState.Idle)
    val uiState: StateFlow<UiState<AccountInfoUiModel>> = _uiState.asStateFlow()

    fun load() {
        launchUiState(_uiState) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("未找到当前用户")
            AccountInfoUiModel(
                userName = user.name,
                userCode = user.code,
                customerCode = user.customerCode,
                customerName = user.customer,
            )
        }
    }
}
