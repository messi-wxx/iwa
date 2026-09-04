package com.cq.iwa.core.network.interceptor

import com.cq.iwa.core.monitor.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineInterceptor @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IOException("网络连接失败，请检查网络")
        }
        return chain.proceed(chain.request())
    }
}
