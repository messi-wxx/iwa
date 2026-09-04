package com.cq.iwa.calibration

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.showImageViewer
import com.cq.iwa.core.media.CapturePhotoHelper
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.databinding.ActivityAddLocationBinding
import com.cq.iwa.feature.calibration.ui.AddLocationViewModel
import com.cq.iwa.media.EnvImageNavigator
import com.cq.iwa.readmeter.MeterPhotos
import com.cq.iwa.readmeter.PhotoAdapter
import com.cq.iwa.replacemeter.AmapLocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AddLocationActivity : IwaBaseActivity<ActivityAddLocationBinding>() {

    private val viewModel: AddLocationViewModel by viewModels()
    @Inject lateinit var appSettings: AppSettings
    private lateinit var captureHelper: CapturePhotoHelper
    private lateinit var locationHelper: AmapLocationHelper
    private lateinit var photoAdapter: PhotoAdapter
    private var mapCreated = false
    private var cameraLatLng: LatLng? = null
    private var gpsCentered = false
    private val editEnvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val path = result.data?.getStringExtra(EnvImageNavigator.EXTRA_PHOTO_PATH)
        if (!path.isNullOrBlank()) {
            viewModel.addPhoto(path)
            photoAdapter.submit(viewModel.photos())
        }
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityAddLocationBinding =
        ActivityAddLocationBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        captureHelper = CapturePhotoHelper(this)
        locationHelper = AmapLocationHelper(this, appSettings) { latLng ->
            if (!mapCreated) return@AmapLocationHelper
            gpsCentered = true
            binding.mapView.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
        binding.mapView.onCreate(savedInstanceState)
        mapCreated = true
        binding.tvAddress.text = intent.getStringExtra(MeterCalibrationNavigator.EXTRA_ADDRESS)
        photoAdapter = PhotoAdapter(
            onAdd = { choosePhoto() },
            onPreview = { image, position, items -> preview(image, position, items) },
            onDelete = { confirmDelete(it) },
        )
        binding.rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotos.adapter = photoAdapter
        binding.btnBack.setOnClickListener { confirmExit() }
        binding.btnSubmit.setOnClickListener { submit() }
        binding.btnRelocate.setOnClickListener {
            gpsCentered = false
            locationHelper.start(once = true)
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmExit()
            },
        )
        lifecycleScope.launch {
            val denied = permissionRequester.request(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            if (denied.isNotEmpty()) {
                showToast("您拒绝了使用此功能必须的定位权限")
            }
            initMap()
        }
    }

    private fun initMap() {
        val map = binding.mapView.map
        map.uiSettings.isZoomControlsEnabled = false
        map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) {
                cameraLatLng = position?.target
            }

            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                cameraLatLng = position?.target
            }
        })
        map.setOnMapLoadedListener {
            locationHelper.latLng().takeIf { it.latitude != 0.0 || it.longitude != 0.0 }?.let {
                gpsCentered = true
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
            }
            locationHelper.start(once = true)
        }
    }

    private fun choosePhoto() {
        IwaDialogs.list(
            this,
            getString(R.string.replacemeter_pick_photo),
            listOf(
                getString(R.string.replacemeter_pick_album),
                getString(R.string.replacemeter_pick_camera),
            ),
        ) { which ->
            if (which == 0) pickFromAlbum() else takePhoto()
        }
    }

    private fun pickFromAlbum() {
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
        editEnvLauncher.launch(
            EnvImageNavigator.edit(
                context = this,
                sourcePath = source.absolutePath,
                outputPath = output.absolutePath,
                overlayText = binding.tvAddress.text?.toString().orEmpty(),
            ),
        )
    }

    private fun preview(image: ImageView, position: Int, items: List<String>) {
        showImageViewer(image, position, MeterPhotos.sources(items)) {
            photoAdapter.imageAt(binding.rvPhotos, it)
        }
    }

    private fun confirmDelete(index: Int) {
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.readmeter_delete_photo_title),
            message = getString(R.string.readmeter_delete_photo_message),
            confirmText = getString(R.string.readmeter_delete),
            onConfirm = {
                viewModel.removePhoto(index)
                photoAdapter.submit(viewModel.photos())
            },
        )
    }

    private fun confirmExit() {
        val remark = binding.etRemark.text?.toString().orEmpty()
        if (!viewModel.hasDraft(remark)) {
            finish()
            return
        }
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.calibration_exit_title),
            message = getString(R.string.calibration_exit_message),
            confirmText = getString(R.string.calibration_exit_confirm),
            cancelText = getString(R.string.calibration_exit_cancel),
            onConfirm = { finish() },
        )
    }

    private fun submit() {
        viewModel.submit(
            isMeter = intent.getBooleanExtra(MeterCalibrationNavigator.EXTRA_IS_METER, false),
            linkId = intent.getLongExtra(MeterCalibrationNavigator.EXTRA_LINK_ID, 0),
            meterCode = intent.getStringExtra(MeterCalibrationNavigator.EXTRA_METER_CODE),
            remark = binding.etRemark.text?.toString().orEmpty(),
            lat = cameraLatLng?.latitude,
            lng = cameraLatLng?.longitude,
            onDone = {
                setResult(RESULT_OK)
                finishAfterToast()
            },
        )
    }

    override fun onResume() {
        super.onResume()
        if (mapCreated) binding.mapView.onResume()
    }

    override fun onPause() {
        if (mapCreated) binding.mapView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mapCreated) binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        locationHelper.stop()
        if (mapCreated) binding.mapView.onDestroy()
        super.onDestroy()
    }
}
