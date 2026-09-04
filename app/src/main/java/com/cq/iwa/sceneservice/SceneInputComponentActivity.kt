package com.cq.iwa.sceneservice

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneInputCodeBinding
import com.cq.iwa.feature.sceneservice.network.SceneJson
import com.cq.iwa.feature.sceneservice.ui.SceneInputComponentViewModel
import com.cq.iwa.scan.BarcodeScanHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneInputComponentActivity : IwaBaseActivity<ActivitySceneInputCodeBinding>() {

    private val viewModel: SceneInputComponentViewModel by viewModels()
    private lateinit var barcodeScanHelper: BarcodeScanHelper

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneInputCodeBinding =
        ActivitySceneInputCodeBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        barcodeScanHelper = BarcodeScanHelper(this, permissionRequester)
        binding.tvTitle.text = getString(R.string.scene_input_component)
        binding.etCode.hint = getString(R.string.scene_component_code)
        viewModel.onFound = { part ->
            setResult(
                SceneServiceNavigator.RESULT_COMPONENT,
                Intent().putExtra(SceneServiceNavigator.EXTRA_PART, SceneJson.encode(part)),
            )
            finish()
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnScan.setOnClickListener {
            barcodeScanHelper.scan { code ->
                binding.etCode.setText(code)
            }
        }
        binding.btnConfirm.setOnClickListener {
            viewModel.query(binding.etCode.text?.toString().orEmpty())
        }
    }
}
