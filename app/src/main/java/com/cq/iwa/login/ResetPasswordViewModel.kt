package com.cq.iwa.login

import com.cq.iwa.R
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.PasswordValidator
import com.cq.iwa.feature.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _resetState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetState: StateFlow<UiState<Unit>> = _resetState.asStateFlow()

    private val _configState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val configState: StateFlow<UiState<Unit>> = _configState.asStateFlow()

    fun resetPassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
    ) {
        when {
            oldPassword.isBlank() -> showToast(R.string.please_fill_old_password)
            newPassword.isBlank() -> showToast(R.string.please_fill_new_password)
            !PasswordValidator.isValid(newPassword) -> showToast(R.string.password_rule)
            confirmPassword.isBlank() -> showToast(R.string.please_confirm_new_password)
            confirmPassword != newPassword -> showToast(R.string.password_confirm_mismatch)
            else -> launchReset(oldPassword, newPassword, confirmPassword)
        }
    }

    private fun launchReset(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
    ) {
        launchUiState(_resetState) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("未找到当前用户")
            when (
                val result = authRepository.resetPassword(
                    code = user.code,
                    oldPassword = oldPassword,
                    password = newPassword,
                    confirmPassword = confirmPassword,
                )
            ) {
                is ApiResult.Success -> authRepository.markPasswordChanged(newPassword)
                is ApiResult.Error -> throw IllegalStateException(result.message)
            }
        }
    }

    fun downloadConfigAfterFirstReset() {
        launchUiState(_configState) {
            when (val result = authRepository.loadAppConfig()) {
                is ApiResult.Success -> authRepository.saveUserConfigs(result.data)
                is ApiResult.Error -> throw IllegalStateException(result.message)
            }
        }
    }
}
