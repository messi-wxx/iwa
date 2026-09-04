package com.cq.iwa.login

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.cq.iwa.R
import com.cq.iwa.core.common.model.UiEvent
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseActivity
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.databinding.ActivityLoginBinding
import com.cq.iwa.home.HomeActivity
import com.cq.iwa.legal.Legal
import com.cq.iwa.sdk.ThirdSdk
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()
    private var passwordVisible = false

    override fun statusBarColorRes(): Int? = R.color.home_status_bar

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivityLoginBinding =
        ActivityLoginBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        val reloginOnly = intent.getBooleanExtra(EXTRA_RELOGIN, false)
        observeUiEvents(viewModel)
        binding.ivCaptcha.setOnClickListener { viewModel.loadCaptcha() }
        binding.btnLogin.setOnClickListener { submitLogin() }
        binding.btnPasswordToggle.setOnClickListener { togglePasswordVisible() }
        bindLoginPrivacyAgree()

        collectUiState(
            stateFlow = viewModel.captchaState,
            onSuccess = { captcha ->
                val bytes = Base64.decode(captcha.image, Base64.DEFAULT)
                binding.ivCaptcha.setImageBitmap(
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size),
                )
            },
            onError = { message ->
                binding.ivCaptcha.setImageDrawable(null)
                showToast(message.ifBlank { getString(R.string.captcha_load_failed) })
            },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is UiState.Loading -> setLoginLoading(true)
                        is UiState.Success -> handleLoginSuccess(reloginOnly)
                        is UiState.Error -> {
                            setLoginLoading(false)
                            showToast(state.message)
                            binding.etCaptcha.text?.clear()
                        }
                        else -> Unit
                    }
                }
            }
        }

        collectUiState(
            stateFlow = viewModel.configState,
            onSuccess = { navigateAfterLogin(reloginOnly) },
            onError = { message ->
                setLoginLoading(false)
                showToast(message)
            },
        )

        collectUiState(
            stateFlow = viewModel.firstLoginState,
            onLoading = { setLoginLoading(true) },
            onSuccess = {
                setLoginLoading(false)
                navigateToResetPassword(
                    oldPassword = binding.etPassword.text?.toString().orEmpty(),
                )
            },
            onError = { message ->
                setLoginLoading(false)
                showToast(message)
            },
        )

        viewModel.loadCaptcha()
    }

    private fun togglePasswordVisible() {
        passwordVisible = !passwordVisible
        setPasswordVisible(binding.etPassword, passwordVisible)
        binding.btnPasswordToggle.setImageResource(
            if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
        )
    }

    private fun setPasswordVisible(editText: EditText, visible: Boolean) {
        val selection = editText.selectionEnd
        editText.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        if (selection >= 0) {
            editText.setSelection(selection)
        }
    }

    private fun bindLoginPrivacyAgree() {
        val link = getString(R.string.privacy_policy_link)
        binding.tvPrivacyAgree.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyAgree.highlightColor = android.graphics.Color.TRANSPARENT
        binding.tvPrivacyAgree.text = Legal.linkAll(
            text = getString(R.string.login_privacy_agree),
            link = link,
            color = getColor(R.color.primary),
        ) {
            Legal.openPrivacyPolicy(this)
        }
    }

    private fun submitLogin() {
        if (!binding.cbPrivacy.isChecked) {
            showToast(getString(R.string.login_privacy_unchecked))
            return
        }
        if (!requireNetwork()) return
        val deviceId = ThirdSdk.resolveDeviceId(this)
        viewModel.login(
            customerCode = binding.etCustomerCode.text?.toString().orEmpty(),
            userCode = binding.etUserCode.text?.toString().orEmpty(),
            password = binding.etPassword.text?.toString().orEmpty(),
            captchaInput = binding.etCaptcha.text?.toString().orEmpty(),
            deviceId = deviceId,
        )
    }

    private fun setLoginLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading
    }

    private fun handleLoginSuccess(reloginOnly: Boolean) {
        val user = viewModel.lastLoginUser ?: return
        when (user.state) {
            -1 -> {
                setLoginLoading(false)
                showToast(getString(R.string.user_disabled))
            }
            0 -> {
                showToast(getString(R.string.first_login_change_password))
                viewModel.prepareFirstLogin(
                    customerCode = binding.etCustomerCode.text?.toString().orEmpty(),
                    password = binding.etPassword.text?.toString().orEmpty(),
                )
            }
            1 -> viewModel.downloadConfig(
                customerCode = binding.etCustomerCode.text?.toString().orEmpty(),
                password = binding.etPassword.text?.toString().orEmpty(),
            )
            else -> {
                setLoginLoading(false)
                showToast(getString(R.string.account_abnormal))
            }
        }
    }

    private fun navigateToResetPassword(oldPassword: String) {
        startActivity(
            Intent(this, ResetPasswordActivity::class.java).apply {
                putExtra(ResetPasswordActivity.EXTRA_FIRST_LOGIN, true)
                putExtra(ResetPasswordActivity.EXTRA_OLD_PASSWORD, oldPassword)
            },
        )
        finish()
    }

    private fun navigateAfterLogin(reloginOnly: Boolean) {
        setLoginLoading(false)
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                if (reloginOnly) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            },
        )
        finish()
    }

    override fun handleUiEvent(event: UiEvent) {
        super.handleUiEvent(event)
        if (event is UiEvent.Toast || event is UiEvent.ToastRes) {
            setLoginLoading(false)
        }
    }

    companion object {
        const val EXTRA_RELOGIN = "relogin"
    }
}
