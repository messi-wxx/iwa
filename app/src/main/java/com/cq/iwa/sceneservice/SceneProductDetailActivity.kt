package com.cq.iwa.sceneservice

import android.os.Bundle
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneProductDetailBinding
import com.cq.iwa.feature.sceneservice.network.SceneProductDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneProductDetailActivity : IwaBaseActivity<ActivitySceneProductDetailBinding>() {

    private var product: SceneProductDto? = null

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneProductDetailBinding =
        ActivitySceneProductDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        val data = SceneServiceNavigator.product(intent)
        if (data == null) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        product = data
        binding.btnBack.setOnClickListener { finish() }
        binding.btnReport.setOnClickListener { SceneServiceNavigator.openReport(this, data.id) }
        binding.btnChange.setOnClickListener {
            if (data.state == 0 && data.currentState == 3) {
                SceneServiceNavigator.openUpdateComponent(this, data.id)
                finish()
            } else {
                showToast(getString(R.string.scene_not_outbound))
            }
        }
        binding.tvProductType.text = "${getString(R.string.scene_product_type)}  ${data.productDefineIdDesc?.name.orEmpty()}"
        binding.tvProductNumber.text = "${getString(R.string.scene_product_number)}  ${data.code.orEmpty()}"
        binding.tvPeriod.text = "${getString(R.string.scene_period)}  ${data.currentStateDesc.orEmpty()}"
        binding.tvState.text = "${getString(R.string.scene_state)}  ${data.stateDesc.orEmpty()}"
        binding.tvCompany.text = "${getString(R.string.scene_company)}  ${data.customersDesc.orEmpty()}"
        binding.tvProductCode.text = "${getString(R.string.scene_product_code)}  ${data.productDefineIdDesc?.code.orEmpty()}"
        binding.tvUse.text = "${getString(R.string.scene_use)}  ${data.useForTypeDesc.orEmpty()}"
    }
}
