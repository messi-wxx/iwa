package com.cq.iwa.sceneservice

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneQueryBinding
import com.cq.iwa.feature.sceneservice.network.SceneQueryResultDto
import com.cq.iwa.feature.sceneservice.ui.SceneQueryViewModel
import com.cq.iwa.readmeter.NfcHelper
import com.cq.iwa.scan.BarcodeScanHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SceneQueryActivity : IwaBaseActivity<ActivitySceneQueryBinding>() {

    private val viewModel: SceneQueryViewModel by viewModels()
    private lateinit var nfcHelper: NfcHelper
    private lateinit var barcodeScanHelper: BarcodeScanHelper

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneQueryBinding =
        ActivitySceneQueryBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        barcodeScanHelper = BarcodeScanHelper(this, permissionRequester)
        observeUiEvents(viewModel)
        viewModel.onEnterFunctions = { SceneServiceNavigator.openFunctions(this, it) }
        binding.btnBack.setOnClickListener { finish() }
        binding.tvCustomer.setOnClickListener { SceneServiceNavigator.openCustomer(this) }
        binding.btnScan.setOnClickListener {
            barcodeScanHelper.scan { code ->
                binding.etMeterCode.setText(normalizeSceneScanCode(code))
            }
        }
        binding.btnQuery.setOnClickListener {
            viewModel.search(binding.etMeterCode.text?.toString().orEmpty())
        }
        binding.btnGo.setOnClickListener { viewModel.goFunctions() }
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                binding.tvCustomer.isVisible = ui.showCustomerPicker
                binding.tvCustomer.text = ui.customerLabel
                bindResult(ui.result, ui.hasResult)
            }
        }
    }

    private fun bindResult(result: SceneQueryResultDto?, hasResult: Boolean) {
        binding.cardResult.isVisible = hasResult
        if (!hasResult || result == null) return
        binding.tvEdc.isVisible = result.edcDeviceInfo != null
        binding.tvEpo.isVisible = result.epoProductInfo != null
        binding.tvItwater.isVisible = result.itWaterMeterInfo != null
        binding.tvDeviceName.text = ""
        binding.tvDeviceIdentify.text = ""
        binding.tvDeviceNumber.text = ""
        binding.tvNameplate.text = ""
        binding.tvAddress.text = ""
        binding.tvCreateDate.text = ""
        when {
            result.edcDeviceInfo != null -> {
                val edc = result.edcDeviceInfo!!
                binding.tvDeviceName.text = edc.deviceName.orEmpty()
                binding.tvDeviceIdentify.text = edc.identityCode.orEmpty()
                binding.tvDeviceNumber.text = edc.deviceCode.orEmpty()
                binding.tvNameplate.text = edc.nameplate.orEmpty()
                binding.tvAddress.text = edc.address.orEmpty()
                binding.tvCreateDate.text = edc.createTime.orEmpty()
            }
            result.epoProductInfo != null -> {
                binding.tvDeviceNumber.text = result.epoProductInfo!!.fullCode.orEmpty()
                binding.tvCreateDate.text = result.epoProductInfo!!.createTime.orEmpty()
            }
            result.itWaterMeterInfo != null -> {
                val it = result.itWaterMeterInfo!!
                binding.tvDeviceNumber.text = it.code.orEmpty()
                binding.tvNameplate.text = it.namePlate.orEmpty()
                binding.tvAddress.text = it.instAddr.orEmpty()
                binding.tvCreateDate.text = it.instDate.orEmpty()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enable()
    }

    override fun onPause() {
        nfcHelper.disable()
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.clearTempToken()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcHelper.readMeterCode(intent)?.let { raw ->
            binding.etMeterCode.setText(normalizeSceneScanCode(raw))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SceneServiceNavigator.REQUEST_CUSTOMER &&
            resultCode == SceneServiceNavigator.RESULT_CUSTOMER &&
            data != null
        ) {
            viewModel.selectCustomer(
                data.getStringExtra("customerText").orEmpty(),
                data.getStringExtra("customerValue").orEmpty(),
            )
        }
    }
}
