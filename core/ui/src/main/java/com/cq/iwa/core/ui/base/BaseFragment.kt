package com.cq.iwa.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.cq.iwa.core.common.model.UiEvent
import com.cq.iwa.core.ui.R
import com.cq.iwa.core.ui.toast.ToastUtils
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    abstract fun initView()

    open fun collectEvents() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        collectEvents()
    }

    protected fun observeUiEvents(viewModel: BaseViewModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleUiEvent(it) }
            }
        }
    }

    protected open fun handleUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.Toast -> showToast(event.message)
            is UiEvent.ToastRes -> showToast(event.messageRes)
            is UiEvent.ShowMessage -> showMessageDialog(event.title, event.message)
            is UiEvent.ShowLoading -> (activity as? BaseActivity<*>)?.showLoading()
            is UiEvent.HideLoading -> (activity as? BaseActivity<*>)?.hideLoading()
            is UiEvent.Navigate -> onNavigate(event.route)
        }
    }

    protected open fun onNavigate(route: String) {}

    protected fun showToast(message: String) {
        val host = activity ?: return
        if (host.isDestroyed) return
        ToastUtils.show(host.applicationContext, message)
    }

    protected fun showToast(@StringRes messageRes: Int) {
        val host = activity ?: return
        if (host.isDestroyed) return
        ToastUtils.show(host.applicationContext, getString(messageRes))
    }

    private fun showMessageDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.core_dialog_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
