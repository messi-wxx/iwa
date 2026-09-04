package com.cq.iwa.core.common.exception

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 应用统一异常体系
 */
sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NetworkException(message: String = "网络连接失败，请检查网络", cause: Throwable? = null) :
        AppException(message, cause)

    class ServerException(val code: Int, message: String) : AppException(message)

    class TokenExpiredException(message: String = "登录已过期，请重新登录") : AppException(message)

    class ParseException(message: String = "数据解析失败", cause: Throwable? = null) :
        AppException(message, cause)

    class UnknownException(message: String = "未知错误", cause: Throwable? = null) :
        AppException(message, cause)
}

fun Throwable.toAppException(): AppException = when (this) {
    is AppException -> this
    is SocketTimeoutException -> AppException.NetworkException("请求超时，请稍后重试", this)
    is UnknownHostException -> AppException.NetworkException("网络连接失败，请检查网络", this)
    is ConnectException -> AppException.NetworkException("网络连接失败，请检查网络", this)
    is IOException -> AppException.NetworkException(
        message?.takeIf { it.isNotBlank() } ?: "网络连接失败，请检查网络",
        this,
    )
    else -> AppException.UnknownException(message ?: "未知错误", this)
}
