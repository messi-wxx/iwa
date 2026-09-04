package com.cq.iwa.replacemeter

import android.os.Bundle
import android.text.Html
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.databinding.ActivityReplaceMeterMapBinding
import com.cq.iwa.feature.replacemeter.ui.ReplaceMapViewModel
import com.cq.iwa.replacemeter.map.ReplaceClusterOverlay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplaceMapActivity : IwaBaseActivity<ActivityReplaceMeterMapBinding>() {

    private val viewModel: ReplaceMapViewModel by viewModels()
    @Inject lateinit var appSettings: AppSettings
    private lateinit var locationHelper: AmapLocationHelper
    private var overlay: ReplaceClusterOverlay? = null
    private var aMap: AMap? = null
    private var mapCreated = false

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityReplaceMeterMapBinding =
        ActivityReplaceMeterMapBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        locationHelper = AmapLocationHelper(this, appSettings) { latLng ->
            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
        binding.mapView.onCreate(savedInstanceState)
        mapCreated = true
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRelocate.setOnClickListener {
            locationHelper.start(once = true)
        }
        lifecycleScope.launch {
            val denied = permissionRequester.request(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            if (denied.isNotEmpty()) {
                showToast("您拒绝了使用此功能必须的定位权限")
            }
            initMap()
            if (denied.isEmpty()) locationHelper.start(once = true)
        }
        collectPageState(
            stateFlow = viewModel.uiState,
            showLoadingOverlay = false,
            onSuccess = { ui ->
                if (ui.taskName.isNotBlank()) binding.tvTitle.text = ui.taskName
                binding.tvTip.text = Html.fromHtml(
                    getString(R.string.replacemeter_map_tip, ui.unfinished, ui.finished, ui.notNeed),
                    Html.FROM_HTML_MODE_LEGACY,
                )
                overlay?.setMeters(ui.meters)
            },
        )
    }

    private fun initMap() {
        if (aMap != null) return
        aMap = binding.mapView.map.apply {
            uiSettings.isZoomControlsEnabled = false
            overlay = ReplaceClusterOverlay(this@ReplaceMapActivity, this) { tableId, replaceable ->
                if (replaceable) {
                    ReplaceMeterNavigator.openDetail(this@ReplaceMapActivity, tableId)
                } else {
                    showToast(getString(R.string.replacemeter_not_replaceable))
                }
            }
            setOnMapLoadedListener {
                locationHelper.latLng().takeIf { it.latitude != 0.0 || it.longitude != 0.0 }?.let {
                    animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
                }
                overlay?.assignClusters()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapCreated) binding.mapView.onResume()
        intent.getStringExtra(ReplaceMeterNavigator.EXTRA_TASK_ID)?.let { viewModel.load(it) }
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
        overlay?.destroy()
        locationHelper.stop()
        if (mapCreated) binding.mapView.onDestroy()
        super.onDestroy()
    }
}
