package com.cq.iwa.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cq.iwa.BuildConfig
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.ui.base.BaseActivity
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.databinding.ActivitySplashBinding
import com.cq.iwa.home.HomeActivity
import com.cq.iwa.legal.Legal
import com.cq.iwa.login.LoginActivity
import com.cq.iwa.login.ResetPasswordActivity
import com.cq.iwa.sdk.ThirdSdk
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    @Inject
    lateinit var appSettings: AppSettings

    private val viewModel: SplashViewModel by viewModels()

    override fun statusBarColorRes(): Int? = R.color.home_status_bar

    override fun isLightStatusBar(): Boolean = true

    override fun observeNetworkChanges(): Boolean = false

    override fun inflateBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前调用，才会切到 postSplashScreenTheme（AppCompat）
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        super.onCreate(savedInstanceState)
    }

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        collectUiState(
            stateFlow = viewModel.uiState,
            onSuccess = { destination ->
                navigate(destination)
            },
            onError = { message ->
                showToast(message)
                startActivity(Intent(this, LoginActivity::class.java))
                finishAfterToast()
            },
        )
        if (isPrivacyAgreed()) {
            ThirdSdk.initAfterPrivacy(this, BuildConfig.DEBUG)
            viewModel.bootstrap()
        } else {
            showPrivacyDialog()
        }
    }

    private fun isPrivacyAgreed(): Boolean {
        if (appSettings.privacyAgreed) return true
        val legacy = getSharedPreferences(PREFS_APP_CONFIG, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRIVACY_AGREED, false)
        if (legacy) {
            appSettings.privacyAgreed = true
        }
        return appSettings.privacyAgreed
    }

    private fun showPrivacyDialog() {
        val link = getString(R.string.privacy_policy_link)
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.privacy_title),
            message = Legal.linkAll(
                text = getString(R.string.privacy_message),
                link = link,
                color = getColor(R.color.primary),
            ) {
                Legal.openPrivacyPolicy(this)
            },
            confirmText = getString(R.string.privacy_agree),
            cancelText = getString(R.string.privacy_disagree),
            cancelable = false,
            onConfirm = {
                appSettings.privacyAgreed = true
                ThirdSdk.initAfterPrivacy(this, BuildConfig.DEBUG)
                viewModel.bootstrap()
            },
            onCancel = { finishAffinity() },
        )
    }

    private fun navigate(destination: SplashDestination) {
        val intent = when (destination) {
            SplashDestination.Home -> Intent(this, HomeActivity::class.java)
            SplashDestination.Login -> Intent(this, LoginActivity::class.java)
            SplashDestination.ResetPassword -> Intent(this, ResetPasswordActivity::class.java).apply {
                putExtra(ResetPasswordActivity.EXTRA_FIRST_LOGIN, true)
            }
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val PREFS_APP_CONFIG = "app_config"
        private const val KEY_PRIVACY_AGREED = "privacy_agreed"
    }
}

enum class SplashDestination {
    Home,
    Login,
    ResetPassword,
}
