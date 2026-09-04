package com.cq.iwa.splash

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.sdk.ThirdSdk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<SplashDestination>>(UiState.Idle)
    val uiState: StateFlow<UiState<SplashDestination>> = _uiState.asStateFlow()

    fun bootstrap() {
        launchUiState(_uiState) {
            val user = authRepository.restoreSessionFromDb()
            if (user != null) {
                ThirdSdk.setUserId(user.customerCode, user.code)
            }
            when {
                user == null -> SplashDestination.Login
                user.state == 1 -> SplashDestination.Home
                user.state == 0 -> SplashDestination.ResetPassword
                else -> SplashDestination.Login
            }
        }
    }
}
