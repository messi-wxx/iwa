package com.cq.iwa.sceneservice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivitySceneMeterChangeBinding
import com.cq.iwa.feature.sceneservice.ui.SceneMeterChangeForm
import com.cq.iwa.feature.sceneservice.ui.SceneMeterChangeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneMeterChangeActivity : IwaBaseActivity<ActivitySceneMeterChangeBinding>() {

    private val viewModel: SceneMeterChangeViewModel by viewModels()
    private var bindingForm = false

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneMeterChangeBinding =
        ActivitySceneMeterChangeBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val type = intent.getIntExtra(SceneServiceNavigator.EXTRA_REPLACE_TYPE, 1)
        val info = SceneServiceNavigator.deviceInfo(intent)
        if (info == null) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        binding.tvTitle.text = when (type) {
            2 -> getString(R.string.scene_replace_convey)
            3 -> getString(R.string.scene_replace_convey_valve)
            else -> getString(R.string.scene_replace_common)
        }
        binding.tvConveyType.isVisible = type != 1
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { viewModel.submit() }
        viewModel.onSaved = {
            setResult(RESULT_OK)
            finish()
        }
        viewModel.onPickBook = { SceneServiceNavigator.openBookChoice(this, it) }
        viewModel.onPickDict = { kind, list ->
            if (list.isEmpty()) {
                showToast("无数据")
            } else {
                IwaDialogs.list(this, "", list.map { it.label.orEmpty() }) { which ->
                    viewModel.applyDict(kind, list[which])
                    bindForm(viewModel.form.value)
                }
            }
        }
        viewModel.onPickNameplate = { list ->
            IwaDialogs.list(this, getString(R.string.scene_nameplate), list.map { it.label.orEmpty() }) { which ->
                viewModel.applyNameplate(list[which])
                bindForm(viewModel.form.value)
            }
        }
        bindClicks()
        bindWatchers()
        viewModel.bind(info, type)
        bindForm(viewModel.form.value)
    }

    private fun bindForm(form: SceneMeterChangeForm) {
        bindingForm = true
        setText(binding.tvReplaceType, form.replaceTypeLabel)
        setText(binding.etReplaceReason, form.replaceReason)
        setText(binding.etDeviceName, form.deviceName)
        setText(binding.etDeviceNumber, form.deviceNumber)
        setText(binding.etIdentify, form.identifyNumber)
        setText(binding.tvNameplate, form.nameplate)
        setText(binding.tvTag, form.tagText)
        setText(binding.etRemark, form.remark)
        setText(binding.tvConveyType, form.conveyTypeLabel)
        setText(binding.tvValveType, form.valveTypeLabel)
        setText(binding.etValveNumber, form.valveNumber)
        setText(binding.tvValveState, form.valveStateLabel)
        setText(binding.tvUseType, form.useTypeLabel)
        setText(binding.tvPrepay, form.prepayLabel)
        setText(binding.etBusiness, form.businessNumber)
        setText(binding.etStamp, form.steelStampNumber)
        setText(binding.tvCaliber, form.caliberLabel)
        setText(binding.etWarningFlux, form.warningFlux)
        setText(binding.etMaxHourFlux, form.maxHourFlux)
        setText(binding.etInitFlux, form.initFlux)
        setText(binding.etMaxFlux, form.maxFlux)
        setText(binding.tvInstallWay, form.installWayLabel)
        setText(binding.etRatio, form.ratio)
        setText(binding.etCommonFlux, form.commonFlux)
        setText(binding.tvBook, form.bookName)
        bindingForm = false
    }

    private fun bindClicks() {
        binding.tvReplaceType.setOnClickListener { viewModel.pickReplaceType() }
        binding.tvNameplate.setOnClickListener { viewModel.pickNameplate() }
        binding.tvTag.setOnClickListener { viewModel.pickTag() }
        binding.tvConveyType.setOnClickListener { viewModel.pickConveyType() }
        binding.tvValveType.setOnClickListener { viewModel.pickValveType() }
        binding.tvValveState.setOnClickListener { viewModel.pickValveState() }
        binding.tvUseType.setOnClickListener { viewModel.pickUseType() }
        binding.tvPrepay.setOnClickListener { viewModel.pickPrepay() }
        binding.tvCaliber.setOnClickListener { viewModel.pickCaliber() }
        binding.tvInstallWay.setOnClickListener { viewModel.pickInstallWay() }
        binding.tvBook.setOnClickListener { viewModel.pickBook() }
    }

    private fun bindWatchers() {
        watch(binding.etReplaceReason) { form, text -> form.copy(replaceReason = text) }
        watch(binding.etDeviceName) { form, text -> form.copy(deviceName = text) }
        watch(binding.etDeviceNumber) { form, text -> form.copy(deviceNumber = text) }
        watch(binding.etIdentify) { form, text -> form.copy(identifyNumber = text) }
        watch(binding.etRemark) { form, text -> form.copy(remark = text) }
        watch(binding.etValveNumber) { form, text -> form.copy(valveNumber = text) }
        watch(binding.etBusiness) { form, text -> form.copy(businessNumber = text) }
        watch(binding.etStamp) { form, text -> form.copy(steelStampNumber = text) }
        watch(binding.etWarningFlux) { form, text -> form.copy(warningFlux = text) }
        watch(binding.etMaxHourFlux) { form, text -> form.copy(maxHourFlux = text) }
        watch(binding.etInitFlux) { form, text -> form.copy(initFlux = text) }
        watch(binding.etMaxFlux) { form, text -> form.copy(maxFlux = text) }
        watch(binding.etRatio) { form, text -> form.copy(ratio = text) }
        watch(binding.etCommonFlux) { form, text -> form.copy(commonFlux = text) }
    }

    private fun watch(view: EditText, transform: (SceneMeterChangeForm, String) -> SceneMeterChangeForm) {
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (bindingForm) return
                viewModel.update { transform(it, s?.toString().orEmpty()) }
            }
        })
    }

    private fun setText(view: TextView, value: String) {
        if (view.text?.toString() != value) view.text = value
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SceneServiceNavigator.REQUEST_BOOK && resultCode == RESULT_OK) {
            SceneServiceNavigator.book(data)?.let {
                viewModel.applyBook(it)
                bindForm(viewModel.form.value)
            }
        }
    }
}
