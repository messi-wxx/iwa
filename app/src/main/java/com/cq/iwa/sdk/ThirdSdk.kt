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
import com.cq.iwa.core.storage.auth.MmkvSessionStore
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

    @Volatile
    private var pendingUserId: String? = null

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
        val id = joinUserId(customerCode, userCode) ?: return
        pendingUserId = id
        if (!::appContext.isInitialized) return
        // 带 Context 的重载在 init 前后都会写入。无 Context 的重载在未初始化时直接丢弃，
        // 设备上会一直留着旧的 ObservableField 对象地址。
        CrashReport.setUserId(appContext, id)
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
        applyUserId()
    }

    private fun applyUserId() {
        if (!::appContext.isInitialized) return
        val id = pendingUserId ?: savedUserId() ?: return
        pendingUserId = id
        CrashReport.setUserId(appContext, id)
    }

    private fun savedUserId(): String? {
        val session = MmkvSessionStore()
        return joinUserId(session.getCustomerCode().orEmpty(), session.getUserCode().orEmpty())
    }

    private fun joinUserId(customerCode: String, userCode: String): String? {
        val customer = accountText(customerCode) ?: return null
        val user = accountText(userCode) ?: return null
        return "$customer-$user"
    }

    private fun accountText(value: String): String? =
        value.trim().takeIf { it.isNotEmpty() && !it.contains("ObservableField") }

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

    private fun isBuglyHostFailure(t: Throwable): Boolean {
        var current: Throwable? = t
        while (current != null) {
            if (current.message.orEmpty().contains("h.trace.qq.com")) return true
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

    /** 接口网络失败上报 Bugly。异常信息里带请求方法和地址，不写本地日志。 */
    fun reportNetworkError(error: Throwable) {
        if (!crashInitialized || !isDeviceOnline() || isBuglyHostFailure(error)) return
        runCatching { CrashReport.postCatchedException(error) }
    }

    /**
     * 带异常的 ERROR 仍可上报。断网类 Timber 日志继续丢掉，
     * 避免 Bugly 自己访问 h.trace.qq.com 失败时再报一次。
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
