package com.cq.iwa.core.ui.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.cq.iwa.core.common.model.UiEvent
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.monitor.NetworkMonitor
import com.cq.iwa.core.permission.PermissionRequester
import com.cq.iwa.core.ui.R
import com.cq.iwa.core.ui.di.CoreUiEntryPoint
import com.cq.iwa.core.ui.ext.applyStatusBar
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.core.ui.toast.ToastUtils
import com.cq.iwa.core.ui.widget.StateLayout
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected lateinit var permissionRequester: PermissionRequester
        private set

    private var loadingDialog: Dialog? = null
    private var networkBanner: TextView? = null
    private var networkMonitor: NetworkMonitor? = null

    abstract fun inflateBinding(): VB

    abstract fun initView(savedInstanceState: Bundle?)

    open fun collectEvents() {}

    /** 闪屏等过渡页可关闭断网横幅。 */
    protected open fun observeNetworkChanges(): Boolean = true

    /** 与页面主背景一致的状态栏颜色；子类按页覆盖。 */
    @ColorRes
    protected open fun statusBarColorRes(): Int? = null

    /** 浅色页面用深色图标；深色页面用浅色图标。 */
    protected open fun isLightStatusBar(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequester = PermissionRequester(this)
        networkMonitor = runCatching {
            EntryPointAccessors.fromApplication(
                applicationContext,
                CoreUiEntryPoint::class.java,
            ).networkMonitor()
        }.getOrNull()
        _binding = inflateBinding()
        setContentView(binding.root)
        applyPageStatusBar()
        initView(savedInstanceState)
        collectEvents()
        observeNetworkState()
    }

    protected fun applyPageStatusBar() {
        val colorRes = statusBarColorRes() ?: return
        applyStatusBar(
            color = ContextCompat.getColor(this, colorRes),
            lightBackground = isLightStatusBar(),
        )
    }

    protected fun observeUiEvents(viewModel: BaseViewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleUiEvent(it) }
            }
        }
    }

    protected fun <T> collectPageState(
        stateFlow: StateFlow<UiState<T>>,
        stateLayout: StateLayout? = null,
        showLoadingOverlay: Boolean = stateLayout == null,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null,
        onEmpty: (() -> Unit)? = null,
        onRetry: (() -> Unit)? = null,
    ) {
        stateLayout?.setOnRetry(onRetry)
        collectUiState(
            stateFlow = stateFlow,
            onLoading = {
                if (stateLayout != null) {
                    stateLayout.showLoading()
                } else if (showLoadingOverlay) {
                    showLoading()
                }
            },
            onSuccess = { data ->
                hideLoading()
                stateLayout?.showContent()
                onSuccess(data)
            },
            onError = { message ->
                hideLoading()
                val offline = !isNetworkAvailable()
                if (stateLayout != null) {
                    if (offline) stateLayout.showOffline(message) else stateLayout.showError(message)
                } else if (onError == null) {
                    showToast(message)
                }
                onError?.invoke(message)
            },
            onEmpty = {
                hideLoading()
                stateLayout?.showEmpty(showRetry = onRetry != null)
                onEmpty?.invoke()
            },
        )
    }

    protected open fun handleUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.Toast -> showToast(event.message)
            is UiEvent.ToastRes -> showToast(event.messageRes)
            is UiEvent.ShowMessage -> showMessageDialog(event.title, event.message)
            is UiEvent.ShowLoading -> showLoading()
            is UiEvent.HideLoading -> hideLoading()
            is UiEvent.Navigate -> onNavigate(event.route)
        }
    }

    protected open fun onNavigate(route: String) {
        // 子类或 Navigation 扩展处理
    }

    protected fun showToast(message: String) {
        if (isDestroyed) return
        ToastUtils.show(applicationContext, message)
    }

    protected fun showToast(@StringRes messageRes: Int) {
        if (isDestroyed) return
        ToastUtils.show(applicationContext, getString(messageRes))
    }

    protected fun finishAfterToast() {
        window.decorView.postDelayed({
            if (!isDestroyed && !isFinishing) finish()
        }, 300)
    }

    fun showLoading(message: CharSequence? = null) {
        if (isFinishing || isDestroyed) return
        val existing = loadingDialog
        if (existing?.isShowing == true) {
            existing.findViewById<TextView>(R.id.tvLoadingMessage)?.text =
                message ?: getString(R.string.core_loading)
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        dialogView.findViewById<TextView>(R.id.tvLoadingMessage)?.text =
            message ?: getString(R.string.core_loading)
        loadingDialog = Dialog(this).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setDimAmount(0.45f)
            show()
        }
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    protected fun isNetworkAvailable(): Boolean = networkMonitor?.isCurrentlyOnline() == true

    protected fun requireNetwork(): Boolean {
        if (isNetworkAvailable()) return true
        showToast(R.string.core_network_offline)
        return false
    }

    private fun showMessageDialog(title: String, message: String) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_message, null)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        view.findViewById<TextView>(R.id.tvDialogMessage).text = message
        view.findViewById<TextView>(R.id.btnDialogPositive).setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.45f)
        val width = (resources.displayMetrics.widthPixels * 0.86f).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun observeNetworkState() {
        if (!observeNetworkChanges()) return
        val monitor = networkMonitor ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                monitor.isOnline.collect { online ->
                    if (online) hideNetworkBanner() else showNetworkBanner()
                }
            }
        }
    }

    private fun showNetworkBanner() {
        val parent = findViewById<ViewGroup>(android.R.id.content) ?: return
        if (networkBanner == null) {
            val density = resources.displayMetrics.density
            networkBanner = TextView(this).apply {
                text = getString(R.string.core_network_offline)
                setBackgroundColor(ContextCompat.getColor(this@BaseActivity, R.color.core_banner_offline))
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 13f
                setPadding(
                    (16 * density).toInt(),
                    (8 * density).toInt(),
                    (16 * density).toInt(),
                    (8 * density).toInt(),
                )
            }
            parent.addView(
                networkBanner,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply { gravity = Gravity.TOP },
            )
        }
        networkBanner?.isVisible = true
    }

    private fun hideNetworkBanner() {
        networkBanner?.isVisible = false
    }

    override fun onDestroy() {
        hideLoading()
        super.onDestroy()
        _binding = null
    }
}
