package com.cq.iwa

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.cq.iwa.core.network.auth.AuthSessionManager
import com.cq.iwa.core.ui.base.BaseActivity
import com.cq.iwa.login.LoginActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 需登录态的页面基类：Token 刷新仍失败（401）时统一跳转登录。
 * 具体 Activity 子类需标注 AndroidEntryPoint。
 */
abstract class IwaBaseActivity<VB : ViewBinding> : BaseActivity<VB>() {

    @Inject
    lateinit var authSessionManager: AuthSessionManager

    override fun collectEvents() {
        super.collectEvents()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authSessionManager.sessionExpired.collect {
                    navigateToRelogin()
                }
            }
        }
    }

    protected open fun navigateToRelogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_RELOGIN, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }
}
