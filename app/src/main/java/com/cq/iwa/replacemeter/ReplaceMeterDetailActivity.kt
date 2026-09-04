package com.cq.iwa.replacemeter

import android.content.Intent
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
import com.cq.iwa.core.media.PhotoProcessor
import com.cq.iwa.core.media.VoiceUtil
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.databinding.ActivityReplaceMeterDetailBinding
import com.cq.iwa.feature.replacemeter.ReplaceShowWay
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterDetailUi
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterDetailViewModel
import com.cq.iwa.media.EnvImageNavigator
import com.cq.iwa.readmeter.MeterPhotos
import com.cq.iwa.readmeter.NfcHelper
import com.cq.iwa.readmeter.PhotoAdapter
import com.cq.iwa.readmeter.bindMeterPhoto
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class ReplaceMeterDetailActivity : IwaBaseActivity<ActivityReplaceMeterDetailBinding>() {

    private val viewModel: ReplaceMeterDetailViewModel by viewModels()
    @Inject lateinit var appSettings: AppSettings
    private lateinit var captureHelper: CapturePhotoHelper
    private lateinit var nfcHelper: NfcHelper
    private lateinit var locationHelper: AmapLocationHelper
    private lateinit var envAdapter: PhotoAdapter
    private lateinit var bleHelper: ReplaceBleHelper
    private var photoTarget = PhotoTarget.OLD
    private var currentUi: ReplaceMeterDetailUi? = null
    private val editEnvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val path = result.data?.getStringExtra(EnvImageNavigator.EXTRA_PHOTO_PATH)
        if (!path.isNullOrBlank()) viewModel.addEnvPhoto(path)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityReplaceMeterDetailBinding =
        ActivityReplaceMeterDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        captureHelper = CapturePhotoHelper(this)
        locationHelper = AmapLocationHelper(this, appSettings)
        bleHelper = ReplaceBleHelper(
            activity = this,
            permissionRequester = permissionRequester,
            scope = lifecycleScope,
            toast = ::showToast,
            onCode = { viewModel.applyNfcCode(it) },
        )
        observeUiEvents(viewModel)
        envAdapter = PhotoAdapter(
            onAdd = { photoTarget = PhotoTarget.ENV; chooseEnvPhoto() },
            onPreview = { image, position, items ->
                showImageViewer(image, position, MeterPhotos.sources(items)) {
                    envAdapter.imageAt(binding.rvEnvPhotos, it)
                }
            },
            onDelete = { index ->
                IwaDialogs.confirm(
                    context = this,
                    title = getString(R.string.readmeter_delete_photo_title),
                    message = getString(R.string.readmeter_delete_photo_message),
                    confirmText = getString(R.string.readmeter_delete),
                    onConfirm = { viewModel.removeEnvPhoto(index) },
                )
            },
        )
        binding.rvEnvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEnvPhotos.adapter = envAdapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShowWay.setOnClickListener { pickShowWay() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnBle.setOnClickListener { bleHelper.connectOrScan() }
        binding.btnBuilding.setOnClickListener { openBuilding() }
        binding.oldPhotoBox.setOnClickListener { photoTarget = PhotoTarget.OLD; takePhoto() }
        binding.newPhotoBox.setOnClickListener { photoTarget = PhotoTarget.NEW; takePhoto() }
        binding.oldPhotoBox.setOnLongClickListener {
            if (binding.oldPhotoAdd.isVisible.not()) confirmDelete(PhotoTarget.OLD)
            true
        }
        binding.newPhotoBox.setOnLongClickListener {
            if (binding.newPhotoAdd.isVisible.not()) confirmDelete(PhotoTarget.NEW)
            true
        }
        binding.tvVerifyDate.setOnClickListener { pickDate(true) }
        binding.tvVerifyExpire.setOnClickListener { pickDate(false) }
        binding.etOldReading.addTextChangedListener(simpleWatcher { viewModel.updateDraft(oldReading = it) })
        binding.etNewMeterCode.addTextChangedListener(simpleWatcher { viewModel.updateDraft(newMeterCode = it) })
        binding.etNewReading.addTextChangedListener(simpleWatcher { viewModel.updateDraft(newReading = it) })
        binding.etCaliber.addTextChangedListener(simpleWatcher { viewModel.updateDraft(caliber = it) })
        binding.etVerifyOrg.addTextChangedListener(simpleWatcher { viewModel.updateDraft(verifyOrg = it) })
        binding.rgInstallType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.rbVertical) "立式" else "卧式"
            viewModel.updateDraft(installType = type)
        }
        collectPageState(viewModel.uiState, showLoadingOverlay = false, onSuccess = ::bind)
        viewModel.load(intent.getLongExtra(ReplaceMeterNavigator.EXTRA_TABLE_ID, -1))
        lifecycleScope.launch {
            permissionRequester.request(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            locationHelper.start(once = false)
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
        bleHelper.destroy()
        locationHelper.stop()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcHelper.readMeterCode(intent)?.let(viewModel::applyNfcCode)
    }

    private fun bind(ui: ReplaceMeterDetailUi) {
        currentUi = ui
        binding.tvAddress.text = ui.address
        binding.tvClientCode.text = ui.clientCode.ifBlank { getString(R.string.readmeter_placeholder) }
        binding.tvChargeRead.text = ui.replaceRyFlux.ifBlank { getString(R.string.readmeter_placeholder) }
        binding.tvOldMeterCode.text = ui.oldMeterCode
        setTextIfChanged(binding.etOldReading, ui.oldReading)
        setTextIfChanged(binding.etNewMeterCode, ui.newMeterCode)
        setTextIfChanged(binding.etNewReading, ui.newReading)
        setTextIfChanged(binding.etCaliber, ui.caliber)
        setTextIfChanged(binding.etVerifyOrg, ui.verifyOrg)
        if (binding.tvVerifyDate.text?.toString() != ui.verifyDate) {
            binding.tvVerifyDate.text = ui.verifyDate
        }
        if (binding.tvVerifyExpire.text?.toString() != ui.verifyExpireDate) {
            binding.tvVerifyExpire.text = ui.verifyExpireDate
        }
        binding.oldCard.isVisible = ui.showOld
        binding.newCard.isVisible = ui.showNew
        bindSinglePhoto(ui.oldPhoto, binding.ivOldPhoto, binding.oldPhotoAdd)
        bindSinglePhoto(ui.newPhoto, binding.ivNewPhoto, binding.newPhotoAdd)
        envAdapter.submit(ui.envPhotos)
        val wantHorizontal = ui.installType == "卧式"
        if (wantHorizontal) {
            if (!binding.rbHorizontal.isChecked) binding.rbHorizontal.isChecked = true
        } else if (!binding.rbVertical.isChecked) {
            binding.rbVertical.isChecked = true
        }
        intent.putExtra(ReplaceMeterNavigator.EXTRA_TABLE_ID, ui.tableId)
    }

    private fun bindSinglePhoto(path: String, image: ImageView, add: android.view.View) {
        add.isVisible = path.isBlank()
        image.bindMeterPhoto(path)
    }

    private fun pickShowWay() {
        val options = listOf(
            getString(R.string.replacemeter_way_old),
            getString(R.string.replacemeter_way_new),
            getString(R.string.replacemeter_way_both),
        )
        IwaDialogs.list(this, getString(R.string.replacemeter_show_way), options) { which ->
            viewModel.setShowWay(
                when (which) {
                    0 -> ReplaceShowWay.OLD_ONLY
                    1 -> ReplaceShowWay.NEW_ONLY
                    else -> ReplaceShowWay.BOTH
                },
            )
        }
    }

    private fun save() {
        collectForm()
        val loc = locationHelper.latLng()
        viewModel.validateAndSave(
            latitude = loc.latitude,
            longitude = loc.longitude,
            onNeedConfirm = { tip ->
                IwaDialogs.confirm(
                    context = this,
                    title = getString(R.string.readmeter_dialog_tip),
                    message = tip,
                    confirmText = getString(R.string.readmeter_continue_save),
                    onConfirm = {
                        viewModel.persist(loc.latitude, loc.longitude) { close ->
                            onReplaceSaved(close)
                        }
                    },
                )
            },
            onSaved = { close -> onReplaceSaved(close) },
        )
    }

    private fun collectForm() {
        viewModel.updateDraft(
            oldReading = binding.etOldReading.text?.toString(),
            newMeterCode = binding.etNewMeterCode.text?.toString(),
            newReading = binding.etNewReading.text?.toString(),
            caliber = binding.etCaliber.text?.toString(),
            verifyOrg = binding.etVerifyOrg.text?.toString(),
            verifyDate = binding.tvVerifyDate.text?.toString(),
            verifyExpireDate = binding.tvVerifyExpire.text?.toString(),
            installType = if (binding.rbHorizontal.isChecked) "卧式" else "立式",
        )
    }

    private fun onReplaceSaved(close: Boolean) {
        VoiceUtil.play(this)
        if (close) finishAfterToast()
    }

    private fun chooseEnvPhoto() {
        if (!viewModel.canAddEnvPhoto()) {
            showToast("环境图片最多上传四张")
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
            if (which == 0) pickEnvFromAlbum() else takePhoto()
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

    private fun takePhoto() {
        if (photoTarget == PhotoTarget.ENV && !viewModel.canAddEnvPhoto()) {
            showToast("环境图片最多上传四张")
            return
        }
        lifecycleScope.launch {
            if (!permissionRequester.request(android.Manifest.permission.CAMERA)) {
                showToast("您拒绝了使用此功能必须的权限")
                return@launch
            }
            val captured = viewModel.createPictureFile()
            val uri = captureHelper.capture(captured)
            if (uri == null) return@launch
            when (photoTarget) {
                PhotoTarget.OLD, PhotoTarget.NEW -> {
                    showLoading(getString(R.string.replacemeter_saving_image))
                    val marked = viewModel.createPictureFile()
                    val ok = runCatching {
                        PhotoProcessor.addTimeWatermark(captured, marked)
                        PhotoProcessor.compressIfNeeded(marked)
                    }.isSuccess
                    hideLoading()
                    captured.delete()
                    if (!ok) {
                        showToast(getString(R.string.replacemeter_edit_env_fail))
                        return@launch
                    }
                    if (photoTarget == PhotoTarget.OLD) {
                        viewModel.setOldPhoto(marked.absolutePath)
                    } else {
                        viewModel.setNewPhoto(marked.absolutePath)
                    }
                }
                PhotoTarget.ENV -> openEnvEditor(captured)
            }
        }
    }

    private suspend fun openEnvEditor(source: File) {
        val output = viewModel.createPictureFile()
        editEnvLauncher.launch(
            EnvImageNavigator.edit(
                context = this,
                sourcePath = source.absolutePath,
                outputPath = output.absolutePath,
                overlayText = envOverlayText(),
            ),
        )
    }

    private fun envOverlayText(): String {
        val code = binding.etNewMeterCode.text?.toString().orEmpty()
        val addr = currentUi?.placeAddress.orEmpty()
        return EnvImageNavigator.overlayText(code, addr)
    }

    private fun confirmDelete(target: PhotoTarget) {
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.readmeter_delete_photo_title),
            message = getString(R.string.readmeter_delete_photo_message),
            confirmText = getString(R.string.readmeter_delete),
            onConfirm = {
                when (target) {
                    PhotoTarget.OLD -> viewModel.removeOldPhoto()
                    PhotoTarget.NEW -> viewModel.removeNewPhoto()
                    PhotoTarget.ENV -> Unit
                }
            },
        )
    }

    private fun pickDate(verify: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.replacemeter_pick_date))
            .setNegativeButtonText("清除")
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val text = format.format(Date(millis))
            if (verify) {
                binding.tvVerifyDate.text = text
                viewModel.updateDraft(verifyDate = text)
            } else {
                binding.tvVerifyExpire.text = text
                viewModel.updateDraft(verifyExpireDate = text)
            }
        }
        picker.addOnNegativeButtonClickListener {
            if (verify) {
                binding.tvVerifyDate.text = ""
                viewModel.updateDraft(verifyDate = "")
            } else {
                binding.tvVerifyExpire.text = ""
                viewModel.updateDraft(verifyExpireDate = "")
            }
        }
        picker.show(supportFragmentManager, "replaceDate")
    }

    private fun openBuilding() {
        val code = currentUi?.oldMeterCode.orEmpty()
        if (code.isBlank()) {
            showToast(getString(R.string.replacemeter_need_old_code))
            return
        }
        MeterCalibrationNavigator.openForMeter(this, code)
    }

    private fun setTextIfChanged(view: android.widget.EditText, value: String) {
        if (view.text?.toString() != value) {
            view.setText(value)
            view.setSelection(value.length)
        }
    }

    private fun simpleWatcher(block: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            block(s?.toString().orEmpty())
        }
    }

    private enum class PhotoTarget { OLD, NEW, ENV }
}
