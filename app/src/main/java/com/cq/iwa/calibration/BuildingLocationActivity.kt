package com.cq.iwa.calibration

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.showImageViewer
import com.cq.iwa.databinding.ActivityBuildingLocationBinding
import com.cq.iwa.databinding.ItemLocationPathBinding
import com.cq.iwa.feature.calibration.ui.BuildingLocationViewModel
import com.cq.iwa.feature.calibration.ui.BuildingNodeUi
import com.cq.iwa.readmeter.MeterPhotos
import com.cq.iwa.readmeter.bindMeterPhoto
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent

@AndroidEntryPoint
class BuildingLocationActivity : IwaBaseActivity<ActivityBuildingLocationBinding>() {

    private val viewModel: BuildingLocationViewModel by viewModels()
    private var mapCreated = false
    private var markedPoint: LatLng? = null
    private val pathAdapter = PathAdapter { viewModel.select(it) }
    private val photoAdapter = GuidPhotoAdapter { image, position, items ->
        showImageViewer(image, position, MeterPhotos.sources(items)) { null }
    }
    private val addLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.reloadSelected()
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityBuildingLocationBinding =
        ActivityBuildingLocationBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.mapView.onCreate(savedInstanceState)
        mapCreated = true
        binding.mapView.map.uiSettings.isZoomControlsEnabled = false
        binding.mapView.map.setOnMapLoadedListener {
            markedPoint = null
            val ui = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)?.data
            updateMarker(ui?.lat, ui?.lng)
        }
        binding.mapView.map.setOnMarkerClickListener { marker ->
            val point = marker.position
            IwaDialogs.list(
                this,
                getString(R.string.replacemeter_navi),
                listOf(getString(R.string.replacemeter_navi_amap), getString(R.string.replacemeter_navi_baidu)),
            ) { which ->
                if (which == 0) openAmap(point) else openBaidu(point)
            }
            true
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.rvPath.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPath.adapter = pathAdapter
        binding.rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotos.adapter = photoAdapter
        binding.btnAdd.setOnClickListener { openAdd() }
        binding.tvHistory.setOnClickListener {
            viewModel.loadHistory { records ->
                if (records.isEmpty()) return@loadHistory
                IwaDialogs.list(this, getString(R.string.replacemeter_history), records.map { it.text.orEmpty() }) { which ->
                    viewModel.loadRecord(records[which].value)
                }
            }
        }
        collectPageState(viewModel.uiState, showLoadingOverlay = false, onSuccess = { ui ->
            pathAdapter.submit(ui.nodes)
            photoAdapter.submit(ui.photos)
            binding.rvPhotos.isVisible = ui.photos.isNotEmpty()
            binding.tvRemark.isVisible = ui.remark.isNotBlank()
            binding.tvRemark.text = if (ui.remark.isBlank()) "" else "描述信息：${ui.remark}"
            updateMarker(ui.lat, ui.lng)
        })
        val meterCode = intent.getStringExtra(MeterCalibrationNavigator.EXTRA_METER_CODE)
        val decoded = MeterCalibrationNavigator.decodeAddress(
            intent.getStringExtra(MeterCalibrationNavigator.EXTRA_LOCATION_JSON),
        )
        when {
            decoded != null -> viewModel.bindPath(decoded, meterCode)
            intent.getBooleanExtra(MeterCalibrationNavigator.EXTRA_REQUIRE_AREA, false) ->
                viewModel.loadFromMeter(meterCode.orEmpty(), requireArea = true)
            !meterCode.isNullOrBlank() -> viewModel.bindPath(null, meterCode)
        }
    }

    override fun onNavigate(route: String) {
        if (route == "close") finish()
    }

    private fun updateMarker(lat: Double?, lng: Double?) {
        val map = binding.mapView.map
        if (lat == null || lng == null || lat <= 0 || lng <= 0) {
            if (markedPoint != null) {
                map.clear()
                markedPoint = null
            }
            return
        }
        val point = LatLng(lat, lng)
        if (markedPoint == point) return
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_un_read)),
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
        markedPoint = point
    }

    private fun openAdd() {
        val ui = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)?.data
        if (ui == null || (!ui.selectedIsMeter && ui.selectedId == 0L)) {
            showToast(getString(R.string.replacemeter_no_node))
            return
        }
        addLauncher.launch(
            Intent(this, AddLocationActivity::class.java)
                .putExtra(MeterCalibrationNavigator.EXTRA_ADDRESS, viewModel.currentPath())
                .putExtra(MeterCalibrationNavigator.EXTRA_LINK_ID, ui.selectedId)
                .putExtra(MeterCalibrationNavigator.EXTRA_IS_METER, ui.selectedIsMeter)
                .putExtra(MeterCalibrationNavigator.EXTRA_METER_CODE, ui.selectedName.takeIf { ui.selectedIsMeter }),
        )
    }

    private fun openAmap(point: LatLng) {
        val uri = Uri.parse("amapuri://route/plan/?dlat=${point.latitude}&dlon=${point.longitude}&dev=0&t=0")
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { showToast("未安装高德地图") }
    }

    private fun openBaidu(point: LatLng) {
        val uri = Uri.parse("baidumap://map/direction?destination=${point.latitude},${point.longitude}&mode=driving")
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { showToast("未安装百度地图") }
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
        if (mapCreated) binding.mapView.onDestroy()
        super.onDestroy()
    }
}

private class PathAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<PathAdapter.Holder>() {
    private var items: List<BuildingNodeUi> = emptyList()

    fun submit(list: List<BuildingNodeUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemLocationPathBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvName.setTextColor(
            holder.itemView.context.getColor(if (item.selected) R.color.primary else R.color.navy),
        )
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemLocationPathBinding) : RecyclerView.ViewHolder(binding.root)
}

private class GuidPhotoAdapter(
    private val onPreview: (ImageView, Int, List<String>) -> Unit,
) : RecyclerView.Adapter<GuidPhotoAdapter.Holder>() {
    private var items: List<String> = emptyList()

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val path = items[position]
        holder.image.bindMeterPhoto(path)
        holder.image.setOnClickListener { onPreview(holder.image, position, items) }
    }

    class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivPhoto)
    }
}
