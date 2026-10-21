package com.cq.iwa.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络失败上报 Bugly，异常信息里带上请求方法和地址。
 * 不走 Timber，因此不会写入本地日志。4xx/5xx 不抛异常，也不上报。
 */
@Singleton
class HttpErrorLogInterceptor @Inject constructor(
    private val reporter: NetworkErrorReporter,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            if (!e.isCallCanceled()) {
                reporter.report(IOException("${request.method} ${request.url}", e))
            }
            throw e
        }
    }
}

private fun IOException.isCallCanceled(): Boolean =
    message.equals("Canceled", ignoreCase = true)
