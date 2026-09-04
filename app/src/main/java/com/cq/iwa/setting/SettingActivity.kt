package com.cq.iwa.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.databinding.ActivitySettingBinding
import com.cq.iwa.login.LoginActivity
import com.cq.iwa.login.ResetPasswordActivity
import com.cq.iwa.update.AppUpdateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingActivity : IwaBaseActivity<ActivitySettingBinding>() {

    private val viewModel: SettingViewModel by viewModels()

    @Inject
    lateinit var appUpdateHelper: AppUpdateHelper

    private var suppressAutoNextCallback = true

    override fun statusBarColorRes(): Int? = R.color.main_background

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivitySettingBinding =
        ActivitySettingBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }

        binding.rowAccount.setOnClickListener {
            startActivity(Intent(this, AccountInfoActivity::class.java))
        }

        binding.rowResetPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        binding.rowClearCache.setOnClickListener {
            IwaDialogs.confirm(
                context = this,
                title = getString(R.string.settings_clear_cache),
                message = getString(R.string.settings_clear_cache_message),
                onConfirm = {
                    lifecycleScope.launch {
                        val size = viewModel.clearMeterCache()
                        binding.tvCacheSize.text = size
                        showToast(getString(R.string.settings_cache_cleared))
                    }
                },
            )
        }

        binding.rowUploadBackup.setOnClickListener {
            viewModel.uploadBackup(this)
        }

        binding.rowCheckUpdate.setOnClickListener {
            viewModel.checkApkVersion()
        }

        binding.rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                viewModel.logoutAndClear()
                startActivity(
                    Intent(this@SettingActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                )
                finish()
            }
        }

        binding.switchAutoNext.setOnCheckedChangeListener { _, checked ->
            if (!suppressAutoNextCallback) {
                viewModel.saveAutoNext(checked)
            }
        }

        collectUiState(
            stateFlow = viewModel.uiState,
            onSuccess = { setting ->
                binding.tvAccountName.text = setting.userName
                binding.tvVersion.text = getString(R.string.settings_version, setting.versionName)
                binding.tvCacheSize.text = setting.cacheSizeText
                suppressAutoNextCallback = true
                binding.switchAutoNext.isChecked = setting.autoNext
                suppressAutoNextCallback = false
            },
            onError = { showToast(it) },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateAvailable.collect { version ->
                    appUpdateHelper.promptUpdate(this@SettingActivity, version)
                }
            }
        }

        viewModel.load(this)
    }
}
