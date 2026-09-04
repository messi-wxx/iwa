package com.cq.iwa.sdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import cn.jiguang.api.utils.JCollectionAuth
import cn.jpush.android.api.JPushInterface
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.tencent.bugly.crashreport.CrashReport
import timber.log.Timber

/**
 * Bugly / 极光 / 高德：仅在用户同意启动页隐私弹框后初始化。
 */
object ThirdSdk {

    private const val BUGLY_APP_ID = "5f1590c408"

    @Volatile
    private var crashInitialized = false

    @Volatile
    private var pushInitialized = false

    @Volatile
    private var buglyTreePlanted = false

    @Volatile
    private var amapPrivacyInitialized = false

    private lateinit var appContext: Context

    fun initAfterPrivacy(context: Context, isDebug: Boolean) {
        val app = context.applicationContext
        initAmapPrivacy(app)
        initBugly(app, isDebug)
    }

    /**
     * 极光 init 会在 Android 13+ 弹出通知权限，须进入首页后再调，避免和隐私框叠在一起。
     */
    fun initPush(context: Context, isDebug: Boolean) {
        initJPush(context.applicationContext, isDebug)
    }

    fun setUserId(customerCode: String, userCode: String) {
        if (!crashInitialized) return
        CrashReport.setUserId("$customerCode-$userCode")
    }

    fun resolveDeviceId(context: Context): String {
        val registrationId = JPushInterface.getRegistrationID(context)
        if (!registrationId.isNullOrBlank()) return registrationId
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }

    private fun initBugly(app: Context, isDebug: Boolean) {
        if (crashInitialized) return
        crashInitialized = true
        appContext = app
        CrashReport.initCrashReport(app, BUGLY_APP_ID, isDebug)
        CrashReport.setDeviceModel(app, Build.MODEL)
        plantBuglyTree()
    }

    private fun isExpectedOfflineFailure(t: Throwable): Boolean {
        var current: Throwable? = t
        while (current != null) {
            val msg = current.message.orEmpty()
            if (current is java.net.UnknownHostException ||
                current is java.net.ConnectException ||
                current is java.net.SocketException ||
                current is java.net.SocketTimeoutException ||
                current is java.io.InterruptedIOException ||
                msg.contains("Unable to resolve host") ||
                msg.contains("h.trace.qq.com") ||
                msg.contains("网络连接失败")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * 高德合规：必须先弹出含高德条款的隐私框，用户同意后才能 updatePrivacyAgree(true)。
     */
    private fun initAmapPrivacy(app: Context) {
        if (amapPrivacyInitialized) return
        amapPrivacyInitialized = true
        MapsInitializer.updatePrivacyShow(app, true, true)
        MapsInitializer.updatePrivacyAgree(app, true)
        runCatching {
            AMapLocationClient.updatePrivacyShow(app, true, true)
            AMapLocationClient.updatePrivacyAgree(app, true)
        }
    }

    private fun initJPush(app: Context, isDebug: Boolean) {
        if (pushInitialized) return
        pushInitialized = true
        runCatching { JCollectionAuth.setAuth(app, true) }
        JPushInterface.setDebugMode(isDebug)
        JPushInterface.init(app)
    }

    /**
     * 仅把带 Throwable 的 ERROR 上报 Bugly。断网、超时等预期网络失败不上报，
     * 避免 Bugly 再去解析 h.trace.qq.com 时把 UnknownHostException 打成崩溃。
     */
    private fun plantBuglyTree() {
        if (buglyTreePlanted) return
        buglyTreePlanted = true
        Timber.plant(object : Timber.Tree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.ERROR

            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (t == null || isExpectedOfflineFailure(t) || !isDeviceOnline()) return
                runCatching { CrashReport.postCatchedException(t) }
            }
        })
    }

    private fun isDeviceOnline(): Boolean {
        if (!::appContext.isInitialized) return false
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
