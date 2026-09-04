package com.cq.iwa.replacemeter.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.cq.iwa.R
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.databinding.CustomUmInfoWindowBinding

data class ReplaceClusterItem(
    val tableId: Long,
    val position: LatLng,
    val replaceState: Int,
    val oldMeterCode: String,
    val oldReading: String,
    val newMeterCode: String,
    val newReading: String,
)

class ReplaceClusterOverlay(
    private val context: Context,
    private val aMap: AMap,
    private val onOpenDetail: (Long, Boolean) -> Unit,
) {
    private val markers = mutableListOf<Marker>()
    private var items: List<ReplaceClusterItem> = emptyList()
    private val drawables = mutableMapOf<Int, Drawable>()
    private val icons = mutableMapOf<Int, BitmapDescriptor>()
    private var infoBinding: CustomUmInfoWindowBinding? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val clusterThread = HandlerThread("replace-cluster").apply { start() }
    private val clusterHandler = Handler(clusterThread.looper)
    private var generation = 0
    @Volatile private var destroyed = false

    private val debounceAssign = Runnable { enqueueCalculate() }

    init {
        aMap.setInfoWindowAdapter(object : AMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): android.view.View {
                val item = marker.`object` as? ReplaceClusterItem ?: return android.view.View(context)
                val binding = infoBinding ?: CustomUmInfoWindowBinding.inflate(LayoutInflater.from(context)).also {
                    infoBinding = it
                }
                binding.oldMeterCode.text = item.oldMeterCode
                binding.oldMeterReading.text = item.oldReading
                binding.newMeterCode.text = item.newMeterCode
                binding.newMeterReading.text = item.newReading
                binding.detailButton.setOnClickListener { onOpenDetail(item.tableId, item.replaceState != -1) }
                return binding.root
            }

            override fun getInfoContents(marker: Marker): android.view.View? = null
        })
        aMap.setOnMarkerClickListener { marker ->
            val cluster = marker.`object`
            if (cluster is List<*> && cluster.size > 1) {
                val builder = LatLngBounds.Builder()
                cluster.filterIsInstance<ReplaceClusterItem>().forEach { builder.include(it.position) }
                aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80))
                true
            } else {
                marker.showInfoWindow()
                true
            }
        }
        aMap.setOnMapClickListener {
            markers.forEach { it.hideInfoWindow() }
        }
        aMap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) = Unit
            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                assignClusters()
            }
        })
    }

    fun setMeters(meters: List<ReplaceMeterEntity>) {
        val next = meters.filter { it.latitude > 0 && it.longitude > 0 }.map { meter ->
            ReplaceClusterItem(
                tableId = meter.tableId,
                position = LatLng(meter.latitude, meter.longitude, false),
                replaceState = if (meter.isReplace == 0) -1 else meter.progress,
                oldMeterCode = meter.oldMeterCode.orEmpty(),
                oldReading = meter.oldReading.orEmpty(),
                newMeterCode = meter.newMeterCode.orEmpty(),
                newReading = meter.newReading.orEmpty(),
            )
        }
        if (next == items) return
        items = next
        assignClusters()
    }

    fun assignClusters() {
        if (destroyed) return
        mainHandler.removeCallbacks(debounceAssign)
        mainHandler.postDelayed(debounceAssign, 120)
    }

    fun destroy() {
        destroyed = true
        generation++
        mainHandler.removeCallbacksAndMessages(null)
        clusterHandler.removeCallbacksAndMessages(null)
        clusterThread.quitSafely()
        clearMarkers()
        icons.values.forEach { runCatching { it.recycle() } }
        icons.clear()
        drawables.clear()
        infoBinding = null
    }

    private fun enqueueCalculate() {
        if (destroyed) return
        val snapshot = items
        if (snapshot.isEmpty()) {
            generation++
            clearMarkers()
            return
        }
        val zoom = aMap.cameraPosition?.zoom ?: 10f
        val bounds = runCatching { aMap.projection?.visibleRegion?.latLngBounds }.getOrNull() ?: return
        val clusterDistance = (aMap.scalePerPixel.takeIf { it > 0f } ?: return) * 100
        val gen = ++generation
        clusterHandler.post {
            val clusters = calculate(snapshot, zoom, bounds, clusterDistance)
            mainHandler.post {
                if (!destroyed && gen == generation) render(clusters)
            }
        }
    }

    private fun calculate(
        snapshot: List<ReplaceClusterItem>,
        zoom: Float,
        bounds: LatLngBounds,
        clusterDistance: Float,
    ): List<List<ReplaceClusterItem>> {
        val clusters = mutableListOf<MutableList<ReplaceClusterItem>>()
        snapshot.filter { bounds.contains(it.position) }.forEach { item ->
            val host = if (zoom < 19) {
                clusters.firstOrNull { group ->
                    AMapUtils.calculateLineDistance(item.position, group.first().position) < clusterDistance
                }
            } else {
                null
            }
            if (host != null) host.add(item) else clusters.add(mutableListOf(item))
        }
        return clusters
    }

    private fun render(clusters: List<List<ReplaceClusterItem>>) {
        clearMarkers()
        clusters.forEach { group ->
            val marker = aMap.addMarker(
                MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .icon(iconFor(group))
                    .position(group.first().position),
            )
            marker.`object` = if (group.size == 1) group.first() else group
            markers.add(marker)
        }
    }

    private fun clearMarkers() {
        markers.forEach {
            it.hideInfoWindow()
            it.remove()
        }
        markers.clear()
    }

    private fun iconFor(group: List<ReplaceClusterItem>): BitmapDescriptor {
        val key = if (group.size == 1) {
            when {
                group.first().replaceState > -1 && group.first().replaceState < 3 -> -1
                group.first().replaceState == -1 -> -2
                else -> -3
            }
        } else {
            group.size
        }
        return icons.getOrPut(key) { BitmapDescriptorFactory.fromView(makeIcon(group)) }
    }

    private fun makeIcon(group: List<ReplaceClusterItem>): TextView {
        val background = drawableFor(group)
        val view = TextView(context)
        if (group.size > 1) view.text = group.size.toString()
        view.gravity = Gravity.CENTER
        view.setTextColor(Color.BLACK)
        view.textSize = 15f
        view.background = background
        val size = background.intrinsicWidth.coerceAtLeast(1)
        view.minWidth = size
        view.minHeight = size
        view.layoutParams = ViewGroup.LayoutParams(size, size)
        return view
    }

    private fun drawableFor(group: List<ReplaceClusterItem>): Drawable {
        val count = group.size
        if (count == 1) {
            val state = group.first().replaceState
            val key = when {
                state > -1 && state < 3 -> 1
                state == -1 -> 2
                else -> 3
            }
            return drawables.getOrPut(key) {
                val res = when (key) {
                    1 -> R.mipmap.marker_un_read
                    2 -> R.mipmap.marker_gray
                    else -> R.mipmap.marker_read
                }
                context.resources.getDrawable(res, null)
            }
        }
        val (cacheKey, color) = when {
            count < 5 -> 4 to Color.argb(159, 210, 154, 6)
            count < 10 -> 5 to Color.argb(199, 217, 114, 0)
            else -> 6 to Color.argb(235, 215, 66, 2)
        }
        return drawables.getOrPut(cacheKey) {
            val radius = (80 * context.resources.displayMetrics.density).toInt()
            BitmapDrawable(context.resources, drawCircle(radius, color))
        }
    }

    private fun drawCircle(radius: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawArc(RectF(0f, 0f, radius * 2f, radius * 2f), 0f, 360f, true, paint)
        return bitmap
    }
}
