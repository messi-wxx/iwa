package com.cq.iwa.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 只记录接口超时（含方法 + 完整 URL），并走 Timber.e 上报 Bugly。
 * 4xx/5xx 不写本地日志、不上报，避免把业务错误当成崩溃。
 */
@Singleton
class HttpErrorLogInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            if (e.isTimeout()) {
                val timeout = SocketTimeoutException("${request.method} ${request.url}").apply {
                    initCause(e)
                }
                Timber.e(timeout, "HTTP TIMEOUT %s %s", request.method, request.url)
            }
            throw e
        }
    }
}

private fun Exception.isTimeout(): Boolean = this is SocketTimeoutException
