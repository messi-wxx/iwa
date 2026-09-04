package com.cq.iwa.sceneservice

import android.os.Bundle
import androidx.activity.viewModels
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneSingleReadBinding
import com.cq.iwa.feature.sceneservice.ui.SceneSingleReadViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneSingleReadActivity : IwaBaseActivity<ActivitySceneSingleReadBinding>() {

    private val viewModel: SceneSingleReadViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneSingleReadBinding =
        ActivitySceneSingleReadBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val deviceId = intent.getIntExtra(SceneServiceNavigator.EXTRA_DEVICE_ID, 0)
        val replaceType = intent.getIntExtra(SceneServiceNavigator.EXTRA_REPLACE_TYPE, 0)
        val deviceInfo = SceneServiceNavigator.deviceInfo(intent)
        viewModel.onReplaceContinue = { info ->
            SceneServiceNavigator.openMeterChange(this, info, replaceType)
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            viewModel.save(
                deviceId = deviceId,
                deviceInfo = deviceInfo,
                reading = binding.etReading.text?.toString().orEmpty().trim(),
                displayFlux = binding.etDisplayFlux.text?.toString().orEmpty().trim(),
                positiveFlux = binding.etPositiveFlux.text?.toString().orEmpty().trim(),
                inversionFlux = binding.etInversionFlux.text?.toString().orEmpty().trim(),
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SceneServiceNavigator.REQUEST_REPLACE && resultCode == RESULT_OK) {
            finish()
        }
    }
}
