package com.cq.iwa.sceneservice

import android.os.Bundle
import androidx.activity.viewModels
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneInputCodeBinding
import com.cq.iwa.feature.sceneservice.ui.SceneICodeViewModel
import com.cq.iwa.scan.BarcodeScanHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneInputICodeActivity : IwaBaseActivity<ActivitySceneInputCodeBinding>() {

    private val viewModel: SceneICodeViewModel by viewModels()
    private lateinit var barcodeScanHelper: BarcodeScanHelper

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneInputCodeBinding =
        ActivitySceneInputCodeBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        barcodeScanHelper = BarcodeScanHelper(this, permissionRequester)
        val flag = intent.getIntExtra(SceneServiceNavigator.EXTRA_FLAG, 0)
        val deviceId = intent.getStringExtra(SceneServiceNavigator.EXTRA_DEVICE_ID_STR)
        viewModel.onUpdated = { finish() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnScan.setOnClickListener {
            barcodeScanHelper.scan { code ->
                binding.etCode.setText(code)
            }
        }
        binding.btnConfirm.setOnClickListener {
            viewModel.submit(flag, deviceId, binding.etCode.text?.toString().orEmpty().trim())
        }
    }
}
