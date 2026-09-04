package com.cq.iwa.login

import com.cq.iwa.R
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.network.model.CaptchaDto
import com.cq.iwa.core.network.model.LoginRequest
import com.cq.iwa.core.network.model.LoginUserDto
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.sdk.ThirdSdk
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _captchaState = MutableStateFlow<UiState<CaptchaDto>>(UiState.Idle)
    val captchaState: StateFlow<UiState<CaptchaDto>> = _captchaState.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<LoginUserDto>>(UiState.Idle)
    val loginState: StateFlow<UiState<LoginUserDto>> = _loginState.asStateFlow()

    private val _configState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val configState: StateFlow<UiState<Unit>> = _configState.asStateFlow()

    private val _firstLoginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val firstLoginState: StateFlow<UiState<Unit>> = _firstLoginState.asStateFlow()

    var lastLoginUser: LoginUserDto? = null
        private set

    private var captchaKey: String = ""

    fun loadCaptcha() {
        launchUiState(_captchaState) {
            when (val result = authRepository.getCaptcha()) {
                is ApiResult.Success -> {
                    captchaKey = result.data.answer
                    result.data
                }
                is ApiResult.Error -> throw IllegalStateException(result.message)
            }
        }
    }

    fun login(
        customerCode: String,
        userCode: String,
        password: String,
        captchaInput: String,
        deviceId: String,
    ) {
        when {
            customerCode.isBlank() -> showToast(R.string.please_fill_customer_code)
            userCode.isBlank() -> showToast(R.string.please_fill_account)
            password.isBlank() -> showToast(R.string.please_fill_password)
            captchaInput.isBlank() || captchaKey.isBlank() -> showToast(R.string.please_fill_captcha)
            else -> viewModelScope.launch {
                _loginState.value = UiState.Loading
                val request = LoginRequest(
                    code = customerCode,
                    name = userCode,
                    password = password,
                    answer = captchaInput,
                    captcha = captchaKey,
                    deviceId = deviceId,
                )
                val result = withContext(ioDispatcher) { authRepository.login(request) }
                when (result) {
                    is ApiResult.Success -> {
                        lastLoginUser = result.data
                        _loginState.value = UiState.Success(result.data)
                    }
                    is ApiResult.Error -> {
                        _loginState.value = UiState.Error(result.message)
                        if (result.code == 401 || result.code == 403) {
                            captchaKey = ""
                            loadCaptcha()
                        }
                    }
                }
            }
        }
    }

    fun downloadConfig(customerCode: String, password: String) {
        val user = lastLoginUser ?: return
        launchUiState(_configState) {
            authRepository.persistLogin(user, customerCode, password)
            ThirdSdk.setUserId(customerCode, user.code)
            when (val result = authRepository.loadAppConfig()) {
                is ApiResult.Success -> {
                    authRepository.saveUserConfigs(result.data)
                    showToast(R.string.login_success)
                }
                is ApiResult.Error -> throw IllegalStateException(result.message)
            }
        }
    }

    fun prepareFirstLogin(customerCode: String, password: String) {
        val user = lastLoginUser ?: return
        launchUiState(_firstLoginState) {
            authRepository.persistLogin(user, customerCode, password)
            ThirdSdk.setUserId(customerCode, user.code)
        }
    }
}
