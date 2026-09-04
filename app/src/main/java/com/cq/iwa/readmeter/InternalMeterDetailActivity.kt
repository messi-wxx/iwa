package com.cq.iwa.readmeter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.calibration.MeterCalibrationNavigator
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.showImageViewer
import com.cq.iwa.core.media.CapturePhotoHelper
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.databinding.ActivityMeterDetailInternalBinding
import com.cq.iwa.feature.readmeter.ui.MeterDetailUi
import com.cq.iwa.feature.readmeter.ui.MeterDetailViewModel
import com.cq.iwa.media.EnvImageNavigator
import com.cq.iwa.replacemeter.AmapLocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InternalMeterDetailActivity : IwaBaseActivity<ActivityMeterDetailInternalBinding>() {

    private val viewModel: MeterDetailViewModel by viewModels()
    @Inject lateinit var appSettings: AppSettings
    private lateinit var captureHelper: CapturePhotoHelper
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var envAdapter: PhotoAdapter
    private lateinit var extAdapter: ExtInfoAdapter
    private lateinit var nfcHelper: NfcHelper
    private lateinit var locationHelper: AmapLocationHelper
    private var currentUi: MeterDetailUi? = null
    private val editEnvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val path = result.data?.getStringExtra(EnvImageNavigator.EXTRA_PHOTO_PATH)
        if (!path.isNullOrBlank()) viewModel.addPhoto(path, env = true)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityMeterDetailInternalBinding =
        ActivityMeterDetailInternalBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        captureHelper = CapturePhotoHelper(this)
        locationHelper = AmapLocationHelper(this, appSettings)
        observeUiEvents(viewModel)
        photoAdapter = PhotoAdapter(
            onAdd = { takePhoto() },
            onPreview = { image, position, items -> previewPhotos(image, position, items, false) },
            onDelete = { confirmDeletePhoto(it, false) },
        )
        envAdapter = PhotoAdapter(
            onAdd = { chooseEnvPhoto() },
            onPreview = { image, position, items -> previewPhotos(image, position, items, true) },
            onDelete = { confirmDeletePhoto(it, true) },
        )
        extAdapter = ExtInfoAdapter(
            onCall = { callPhone() },
            onEditPhone = { editPhone() },
            onEditDescribe = { editDescribe() },
            onDebtDate = {},
        )
        binding.rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEnvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvMoreInfo.layoutManager = LinearLayoutManager(this)
        binding.rvPhotos.adapter = photoAdapter
        binding.rvEnvPhotos.adapter = envAdapter
        binding.rvMoreInfo.adapter = extAdapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnPrev.setOnClickListener { viewModel.jump(false) }
        binding.btnNext.setOnClickListener { viewModel.jump(true) }
        binding.btnLast.setOnClickListener { viewModel.jumpLastRead() }
        binding.tvRemark.setOnClickListener { pickRemark() }
        binding.btnBuilding.setOnClickListener { openBuilding() }
        binding.tvPhone.setOnClickListener { callPhone() }
        binding.btnEditPhone.setOnClickListener { editPhone() }
        binding.btnEditDescribe.setOnClickListener { editDescribe() }
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
        val tableId = intent.getLongExtra(MeterNavigator.EXTRA_TABLE_ID, -1)
        viewModel.load(tableId, intent.getBooleanExtra(MeterNavigator.EXTRA_FROM_NFC, false))
        lifecycleScope.launch {
            permissionRequester.request(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            locationHelper.start(once = false)
        }
    }

    override fun onDestroy() {
        locationHelper.stop()
        super.onDestroy()
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
                    intent.getStringExtra(MeterNavigator.EXTRA_PLATFORM).orEmpty(),
                    fromNfc = true,
                )
                finish()
            }
        }
    }

    private fun bind(ui: MeterDetailUi) {
        currentUi = ui
        binding.tvMeterCode.text = ui.meterCode.ifBlank { getString(R.string.readmeter_placeholder) }
        binding.tvCaliber.text = ui.caliber.ifBlank { getString(R.string.readmeter_placeholder) }
        binding.tvAddress.text = ui.address.ifBlank { getString(R.string.readmeter_placeholder) }
        val moreRows = ui.extInfo.filter { it.first != "联系电话" }
        extAdapter.submit(moreRows, showPhoneDescribeActions = false)
        binding.rvMoreInfo.isVisible = moreRows.isNotEmpty()
        binding.tvPhone.text = ui.phone.ifBlank { getString(R.string.readmeter_phone_empty) }
        binding.phoneRow.isVisible = ui.hasExtInfo
        binding.btnEditPhone.isVisible = ui.hasExtInfo
        binding.btnEditDescribe.isVisible = ui.hasExtInfo
        if (binding.etReading.text?.toString() != ui.reading) {
            binding.etReading.setText(ui.reading)
            binding.etReading.setSelection(ui.reading.length)
        }
        binding.tvRemark.text = ui.remark
        binding.tvUsage.text = ui.usageText
        binding.tvUsage.isVisible = ui.usageText.isNotBlank()
        photoAdapter.submit(ui.photos)
        envAdapter.submit(ui.envPhotos)
        binding.envSection.isVisible = ui.showEnvironmentView
        val enabled = ui.nfcUnlocked
        binding.btnSave.isEnabled = enabled
        binding.btnPrev.isEnabled = enabled
        binding.btnNext.isEnabled = enabled
    }

    private fun save() {
        viewModel.updateReading(binding.etReading.text?.toString().orEmpty())
        viewModel.updateRemark(binding.tvRemark.text?.toString().orEmpty())
        val loc = locationHelper.latLng()
        viewModel.validateAndSave(
            latitude = loc.latitude,
            longitude = loc.longitude,
            onNeedConfirm = { tip ->
                IwaDialogs.confirm(
                    context = this,
                    title = getString(R.string.readmeter_dialog_tip),
                    message = "$tip，是否继续保存？",
                    confirmText = getString(R.string.readmeter_continue_save),
                    onConfirm = { viewModel.persist(loc.latitude, loc.longitude) {} },
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
            val granted = permissionRequester.request(android.Manifest.permission.CAMERA)
            if (!granted) {
                showToast("您拒绝了使用此功能必须的权限")
                return@launch
            }
            val file = viewModel.createPictureFile()
            val uri = captureHelper.capture(file)
            if (uri == null) return@launch
            showLoading(getString(R.string.readmeter_saving_image))
            try {
                viewModel.addCapturedPhoto(file, env = false)
            } finally {
                hideLoading()
            }
        }
    }

    private fun chooseEnvPhoto() {
        if (!viewModel.canAddPhoto(true)) {
            showToast("环境照片最多4张")
            return
        }
        IwaDialogs.list(
            this,
            getString(R.string.replacemeter_pick_photo),
            listOf(
                getString(R.string.replacemeter_pick_album),
                getString(R.string.replacemeter_pick_camera),
            ),
        ) { which ->
            if (which == 0) pickEnvFromAlbum() else takeEnvPhoto()
        }
    }

    private fun pickEnvFromAlbum() {
        lifecycleScope.launch {
            val uri = captureHelper.pickImage() ?: return@launch
            val captured = viewModel.createPictureFile()
            val ok = withContext(Dispatchers.IO) { captureHelper.copyUriToFile(uri, captured) }
            if (!ok) {
                showToast(getString(R.string.replacemeter_edit_env_fail))
                return@launch
            }
            openEnvEditor(captured)
        }
    }

    private fun takeEnvPhoto() {
        lifecycleScope.launch {
            if (!permissionRequester.request(android.Manifest.permission.CAMERA)) {
                showToast("您拒绝了使用此功能必须的权限")
                return@launch
            }
            val captured = viewModel.createPictureFile()
            val uri = captureHelper.capture(captured)
            if (uri != null) openEnvEditor(captured)
        }
    }

    private suspend fun openEnvEditor(source: File) {
        val output = viewModel.createPictureFile()
        val ui = currentUi
        editEnvLauncher.launch(
            EnvImageNavigator.edit(
                context = this,
                sourcePath = source.absolutePath,
                outputPath = output.absolutePath,
                overlayText = EnvImageNavigator.overlayText(
                    ui?.meterCode.orEmpty(),
                    ui?.address.orEmpty(),
                ),
            ),
        )
    }

    private fun openBuilding() {
        val code = currentUi?.meterCode.orEmpty()
        if (code.isBlank()) {
            showToast(getString(R.string.replacemeter_need_old_code))
            return
        }
        MeterCalibrationNavigator.openForMeter(this, code)
    }

    private fun previewPhotos(imageView: ImageView, position: Int, items: List<String>, env: Boolean) {
        val recycler = if (env) binding.rvEnvPhotos else binding.rvPhotos
        val adapter = if (env) envAdapter else photoAdapter
        showImageViewer(
            srcView = imageView,
            position = position,
            images = MeterPhotos.sources(items),
            srcViewAt = { adapter.imageAt(recycler, it) },
        )
    }

    private fun confirmDeletePhoto(index: Int, env: Boolean) {
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.readmeter_delete_photo_title),
            message = getString(R.string.readmeter_delete_photo_message),
            confirmText = getString(R.string.readmeter_delete),
            onConfirm = { viewModel.removePhoto(index, env) },
        )
    }

    private fun callPhone() {
        val phone = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)
            ?.data?.phone.orEmpty()
        if (phone.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    private fun editPhone() {
        val phone = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)?.data?.phone.orEmpty()
        IwaDialogs.input(
            context = this,
            title = getString(R.string.readmeter_edit_phone),
            hint = getString(R.string.readmeter_phone_hint),
            value = phone,
            inputType = android.text.InputType.TYPE_CLASS_PHONE,
            onConfirm = { if (it.isNotBlank()) viewModel.changePhone(it) },
        )
    }

    private fun editDescribe() {
        IwaDialogs.input(
            context = this,
            title = getString(R.string.readmeter_reader_remark),
            hint = getString(R.string.readmeter_reader_remark),
            onConfirm = { viewModel.changeDescribe(it) },
        )
    }
}
