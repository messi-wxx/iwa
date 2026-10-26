package com.cq.iwa.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.databinding.ActivityResetPasswordBinding
import com.cq.iwa.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordActivity : IwaBaseActivity<ActivityResetPasswordBinding>() {

    private val viewModel: ResetPasswordViewModel by viewModels()
    private var isFirstLogin = false
    private var firstLoginCompleted = false
    private var oldVisible = false
    private var newVisible = false
    private var confirmVisible = false

    override fun statusBarColorRes(): Int? = R.color.main_background

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivityResetPasswordBinding =
        ActivityResetPasswordBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        isFirstLogin = intent.getBooleanExtra(EXTRA_FIRST_LOGIN, false)
        val oldPassword = intent.getStringExtra(EXTRA_OLD_PASSWORD).orEmpty()

        binding.tvTitle.text = getString(R.string.reset_password_title)
        binding.btnBack.isVisible = true
        binding.btnBack.setOnClickListener { leavePage() }
        if (isFirstLogin) {
            onBackPressedDispatcher.addCallback(this) { leavePage() }
        }
        if (oldPassword.isNotBlank()) {
            binding.etOldPassword.setText(oldPassword)
        }

        observeUiEvents(viewModel)
        binding.btnReset.setOnClickListener { submitReset() }
        binding.btnOldPasswordToggle.setOnClickListener {
            oldVisible = !oldVisible
            togglePassword(binding.etOldPassword, binding.btnOldPasswordToggle, oldVisible)
        }
        binding.btnNewPasswordToggle.setOnClickListener {
            newVisible = !newVisible
            togglePassword(binding.etNewPassword, binding.btnNewPasswordToggle, newVisible)
        }
        binding.btnConfirmPasswordToggle.setOnClickListener {
            confirmVisible = !confirmVisible
            togglePassword(binding.etConfirmPassword, binding.btnConfirmPasswordToggle, confirmVisible)
        }

        collectUiState(
            stateFlow = viewModel.resetState,
            onLoading = { setResetLoading(true) },
            onSuccess = { handleResetSuccess(isFirstLogin) },
            onError = { message ->
                setResetLoading(false)
                showToast(message)
            },
        )

        collectUiState(
            stateFlow = viewModel.configState,
            onLoading = { setResetLoading(true) },
            onSuccess = { navigateToHome() },
            onError = { message ->
                setResetLoading(false)
                showToast(message)
            },
        )
    }

    private fun togglePassword(editText: EditText, button: ImageButton, visible: Boolean) {
        val selection = editText.selectionEnd
        editText.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        if (selection >= 0) {
            editText.setSelection(selection)
        }
        button.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    private fun submitReset() {
        if (!requireNetwork()) return
        viewModel.resetPassword(
            oldPassword = binding.etOldPassword.text?.toString().orEmpty(),
            newPassword = binding.etNewPassword.text?.toString().orEmpty(),
            confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
        )
    }

    private fun setResetLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnReset.isEnabled = !loading
    }

    private fun handleResetSuccess(isFirstLogin: Boolean) {
        if (isFirstLogin) {
            firstLoginCompleted = true
            viewModel.downloadConfigAfterFirstReset()
        } else {
            setResetLoading(false)
            showToast(getString(R.string.reset_password_success))
            finishAfterToast()
        }
    }

    private fun leavePage() {
        if (isFirstLogin && firstLoginCompleted) return
        if (isFirstLogin) {
            lifecycleScope.launch {
                viewModel.abandonFirstLogin()
                finish()
            }
            return
        }
        finish()
    }

    private fun navigateToHome() {
        setResetLoading(false)
        firstLoginCompleted = true
        showToast(getString(R.string.reset_password_success))
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finishAfterToast()
    }

    companion object {
        const val EXTRA_FIRST_LOGIN = "first_login"
        const val EXTRA_OLD_PASSWORD = "old_password"
    }
}
