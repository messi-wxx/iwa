package com.cq.iwa.readmeter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.showImageViewer
import com.cq.iwa.core.media.CapturePhotoHelper
import com.cq.iwa.databinding.ActivityMeterDetailBcpBinding
import com.cq.iwa.feature.readmeter.ui.MeterDetailUi
import com.cq.iwa.feature.readmeter.ui.MeterDetailViewModel
import com.cq.iwa.urgepayment.UrgePaymentNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BcpMeterDetailActivity : IwaBaseActivity<ActivityMeterDetailBcpBinding>() {

    private val viewModel: MeterDetailViewModel by viewModels()
    private lateinit var captureHelper: CapturePhotoHelper
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var extAdapter: ExtInfoAdapter
    private lateinit var nfcHelper: NfcHelper

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityMeterDetailBcpBinding =
        ActivityMeterDetailBcpBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        captureHelper = CapturePhotoHelper(this)
        observeUiEvents(viewModel)
        photoAdapter = PhotoAdapter(
            onAdd = { takePhoto() },
            onPreview = { image, position, items -> previewPhotos(image, position, items) },
            onDelete = { confirmDeletePhoto(it) },
        )
        extAdapter = ExtInfoAdapter(
            onCall = { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) },
            onEditPhone = { showPhoneInput(it) },
            onEditDescribe = { showDescribeInput(it) },
            onDebtDate = { date ->
                binding.tvDebtDate.isVisible = date.isNotBlank()
                binding.tvDebtDate.text = if (date.isNotBlank()) "欠费信息截止时间：$date" else ""
            },
        )
        binding.rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvExtInfo.layoutManager = LinearLayoutManager(this)
        binding.rvPhotos.adapter = photoAdapter
        binding.rvExtInfo.adapter = extAdapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFeeDetail.setOnClickListener { openFeeDetail() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnPrev.setOnClickListener { viewModel.jump(false) }
        binding.btnNext.setOnClickListener { viewModel.jump(true) }
        binding.tvRemark.setOnClickListener { pickRemark() }
        binding.etReading.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateReading(s?.toString().orEmpty())
            }
        })
        collectPageState(
            stateFlow = viewModel.uiState,
            showLoadingOverlay = false,
            onSuccess = { bind(it) },
        )
        viewModel.load(
            intent.getLongExtra(MeterNavigator.EXTRA_TABLE_ID, -1),
            intent.getBooleanExtra(MeterNavigator.EXTRA_FROM_NFC, false),
        )
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enable()
    }

    override fun onPause() {
        nfcHelper.disable()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcHelper.readMeterCode(intent)?.let { code ->
            viewModel.onNfcTag(code) { tableId ->
                MeterNavigator.openDetail(
                    this,
                    tableId,
                    intent.getStringExtra(MeterNavigator.EXTRA_PLATFORM) ?: "bcp",
                    fromNfc = true,
                )
                finish()
            }
        }
    }

    private fun bind(ui: MeterDetailUi) {
        binding.tvTitle.text = listOf(ui.clientName, ui.clientCode).filter { it.isNotBlank() }.joinToString(" ")
        binding.tvMeterCode.text = ui.meterCode.ifBlank { getString(R.string.readmeter_placeholder) }
        binding.tvAddress.text = ui.address.ifBlank { getString(R.string.readmeter_placeholder) }
        if (binding.etReading.text?.toString() != ui.reading) {
            binding.etReading.setText(ui.reading)
            binding.etReading.setSelection(ui.reading.length)
        }
        binding.tvRemark.text = ui.remark
        binding.tvUsage.text = ui.usageText
        binding.tvUsage.isVisible = ui.calculateUsage && ui.usageText.isNotBlank()
        photoAdapter.submit(ui.photos)
        extAdapter.submit(ui.extInfo)
        val enabled = ui.nfcUnlocked
        binding.btnSave.isEnabled = enabled
        binding.btnPrev.isEnabled = enabled
        binding.btnNext.isEnabled = enabled
    }

    private fun openFeeDetail() {
        val code = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)
            ?.data?.clientCode.orEmpty()
        if (code.isBlank()) return
        UrgePaymentNavigator.openDetail(this, code)
    }

    private fun save() {
        viewModel.updateReading(binding.etReading.text?.toString().orEmpty())
        viewModel.updateRemark(binding.tvRemark.text?.toString().orEmpty())
        viewModel.validateAndSave(
            onNeedConfirm = { tip ->
                IwaDialogs.confirm(
                    context = this,
                    title = getString(R.string.readmeter_dialog_tip),
                    message = "$tip，是否继续保存？",
                    confirmText = getString(R.string.readmeter_continue_save),
                    onConfirm = { viewModel.persist {} },
                )
            },
            onSaved = {},
        )
    }

    private fun pickRemark() {
        val options = viewModel.remarkOptions
        if (options.isEmpty()) {
            showToast("暂未配置备注信息")
            return
        }
        IwaDialogs.list(this, getString(R.string.readmeter_pick_remark), options) { which ->
            val selected = options[which]
            if (selected == "其它") {
                IwaDialogs.input(
                    context = this,
                    title = getString(R.string.readmeter_dialog_tip),
                    hint = getString(R.string.readmeter_other_remark_hint),
                    message = getString(R.string.readmeter_other_remark_hint),
                    onConfirm = { text ->
                        if (text.isBlank()) {
                            showToast("请填写无法抄表具体原因")
                        } else {
                            viewModel.updateRemark(text)
                            binding.tvRemark.text = text
                        }
                    },
                )
            } else {
                viewModel.updateRemark(selected)
                binding.tvRemark.text = selected
            }
        }
    }

    private fun takePhoto() {
        if (!viewModel.canAddPhoto(false)) {
            showToast("最多只能上传三张图片")
            return
        }
        lifecycleScope.launch {
            if (!permissionRequester.request(android.Manifest.permission.CAMERA)) {
                showToast("您拒绝了使用此功能必须的权限")
                return@launch
            }
            val file = viewModel.createPictureFile()
            if (captureHelper.capture(file) != null) {
                showLoading(getString(R.string.readmeter_saving_image))
                try {
                    viewModel.addCapturedPhoto(file, false)
                } finally {
                    hideLoading()
                }
            }
        }
    }

    private fun previewPhotos(imageView: ImageView, position: Int, items: List<String>) {
        showImageViewer(
            srcView = imageView,
            position = position,
            images = MeterPhotos.sources(items),
            srcViewAt = { photoAdapter.imageAt(binding.rvPhotos, it) },
        )
    }

    private fun confirmDeletePhoto(index: Int) {
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.readmeter_delete_photo_title),
            message = getString(R.string.readmeter_delete_photo_message),
            confirmText = getString(R.string.readmeter_delete),
            onConfirm = { viewModel.removePhoto(index, false) },
        )
    }

    private fun showPhoneInput(value: String) {
        IwaDialogs.input(
            context = this,
            title = getString(R.string.readmeter_edit_phone),
            hint = getString(R.string.readmeter_phone_hint),
            value = value,
            inputType = android.text.InputType.TYPE_CLASS_PHONE,
            onConfirm = { if (it.isNotBlank()) viewModel.changePhone(it) },
        )
    }

    private fun showDescribeInput(value: String) {
        IwaDialogs.input(
            context = this,
            title = getString(R.string.readmeter_reader_remark),
            hint = getString(R.string.readmeter_reader_remark),
            value = value,
            onConfirm = { viewModel.changeDescribe(it) },
        )
    }
}
