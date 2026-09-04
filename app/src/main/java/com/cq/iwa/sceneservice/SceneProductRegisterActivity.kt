package com.cq.iwa.sceneservice

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivitySceneProductRegisterBinding
import com.cq.iwa.feature.sceneservice.network.ScenePropertyDto
import com.cq.iwa.feature.sceneservice.ui.SceneProductRegisterViewModel
import com.cq.iwa.scan.BarcodeScanHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneProductRegisterActivity : IwaBaseActivity<ActivitySceneProductRegisterBinding>() {

    private val viewModel: SceneProductRegisterViewModel by viewModels()
    private val propertyViews = mutableListOf<PropertyHolder>()
    private lateinit var barcodeScanHelper: BarcodeScanHelper

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneProductRegisterBinding =
        ActivitySceneProductRegisterBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        barcodeScanHelper = BarcodeScanHelper(this, permissionRequester)
        val define = SceneServiceNavigator.define(intent)
        val productId = intent.getStringExtra(SceneServiceNavigator.EXTRA_PRODUCT_ID).orEmpty()
        val partCodes = intent.getStringArrayListExtra(SceneServiceNavigator.EXTRA_PART_CODES).orEmpty()
        if (define == null) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        viewModel.onReplaced = {
            setResult(SceneServiceNavigator.RESULT_REGISTER)
            finish()
        }
        binding.tvProductCode.text = "${getString(R.string.scene_product_code)}  ${define.code.orEmpty()}"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnScan.setOnClickListener {
            barcodeScanHelper.scan { code ->
                binding.etFullCode.setText(code)
            }
        }
        define.propertys.orEmpty().forEach { property ->
            addProperty(property)
        }
        binding.btnDone.setOnClickListener {
            val map = linkedMapOf<String, String>()
            for (holder in propertyViews) {
                val value = holder.value()
                if (holder.property.isRequired && value.isBlank()) {
                    showToast("未填写${holder.property.name.orEmpty()}")
                    return@setOnClickListener
                }
                map[holder.property.code.orEmpty()] = value
            }
            viewModel.submit(
                productId = productId,
                productDefineId = define.id,
                productNumber = binding.etProductNumber.text?.toString().orEmpty().trim(),
                fullCode = binding.etFullCode.text?.toString().orEmpty().trim(),
                partCodes = partCodes,
                properties = map,
            )
        }
    }

    private fun addProperty(property: ScenePropertyDto) {
        val container = binding.propertyContainer
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp), 0, 0)
        }
        val label = TextView(this).apply {
            text = property.name.orEmpty() + if (property.isRequired) " *" else ""
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
        }
        block.addView(label)
        val holder: PropertyHolder = if (property.controlType == 1) {
            val value = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp),
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_input_meter)
                setPadding(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp), 0, 0, 0)
                setTextColor(getColor(R.color.navy))
                val options = property.optionalData.orEmpty().map { it.value.orEmpty() }
                setOnClickListener {
                    if (options.isEmpty()) return@setOnClickListener
                    IwaDialogs.list(this@SceneProductRegisterActivity, property.name.orEmpty(), options) { which ->
                        text = options[which]
                    }
                }
            }
            block.addView(value)
            PropertyHolder(property) { value.text?.toString().orEmpty() }
        } else {
            val input = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp),
                )
                setBackgroundResource(R.drawable.bg_input_meter)
                setPadding(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp), 0, 0, 0)
                setTextColor(getColor(R.color.navy))
                importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
                if (property.dataType != "System.String") {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
            }
            block.addView(input)
            PropertyHolder(property) { input.text?.toString().orEmpty() }
        }
        container.addView(block)
        propertyViews.add(holder)
    }

    private class PropertyHolder(
        val property: ScenePropertyDto,
        val value: () -> String,
    )
}
