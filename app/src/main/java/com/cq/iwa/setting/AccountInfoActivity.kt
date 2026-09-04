package com.cq.iwa.setting

import android.os.Bundle
import androidx.activity.viewModels
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.ui.ext.collectUiState
import com.cq.iwa.databinding.ActivityAccountInfoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountInfoActivity : IwaBaseActivity<ActivityAccountInfoBinding>() {

    private val viewModel: AccountInfoViewModel by viewModels()

    override fun statusBarColorRes(): Int? = R.color.main_background

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivityAccountInfoBinding =
        ActivityAccountInfoBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        collectUiState(
            stateFlow = viewModel.uiState,
            onSuccess = { info ->
                binding.tvUserName.text = info.userName
                binding.tvUserCode.text = info.userCode
                binding.tvCustomerCode.text = info.customerCode
                binding.tvCustomerName.text = info.customerName
            },
            onError = { showToast(it) },
        )
        viewModel.load()
    }
}
