package com.cq.iwa.replacemeter

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.model.LatLng
import com.cq.iwa.core.storage.AppSettings

class AmapLocationHelper(
    context: Context,
    private val appSettings: AppSettings,
    private val onLocated: ((LatLng) -> Unit)? = null,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var client: AMapLocationClient? = null
    private var current: LatLng? = null
    @Volatile private var stopped = true

    fun latLng(): LatLng {
        current?.let { return it }
        val lat = appSettings.lastLatitude.toDoubleOrNull() ?: 0.0
        val lng = appSettings.lastLongitude.toDoubleOrNull() ?: 0.0
        return LatLng(lat, lng)
    }

    fun start(once: Boolean = false) {
        if (!appSettings.privacyAgreed) return
        runCatching {
            stop()
            stopped = false
            val next = AMapLocationClient(appContext)
            next.setLocationOption(
                AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = once
                    isOnceLocationLatest = once
                    if (!once) interval = 5_000
                },
            )
            next.setLocationListener { location ->
                if (stopped) return@setLocationListener
                if (location == null || location.errorCode != 0) {
                    current = null
                    return@setLocationListener
                }
                val latLng = LatLng(location.latitude, location.longitude)
                current = latLng
                appSettings.lastLatitude = location.latitude.toString()
                appSettings.lastLongitude = location.longitude.toString()
                val callback = onLocated ?: return@setLocationListener
                mainHandler.post {
                    if (!stopped) callback(latLng)
                }
            }
            client = next
            next.startLocation()
        }
    }

    fun stop() {
        stopped = true
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            client?.stopLocation()
            client?.onDestroy()
        }
        client = null
    }
}
