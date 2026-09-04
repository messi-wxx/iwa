package com.cq.iwa.core.network

import com.cq.iwa.core.common.exception.AppException
import com.cq.iwa.core.common.model.ApiResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

object ApiExceptionHandler {

    /**
     * 错误响应策略（对齐老项目 BaseViewModel）：
     * - [Standard]：400 返回业务错误；401/403 无 body 时视为登录过期
     * - [Login]：登录接口 401 返回业务错误（密码/验证码错误），不视为登录过期
     */
    enum class ErrorPolicy {
        Standard,
        Login,
    }

    /**
     * 适配服务端「无统一 BaseResponse 包装」的 Retrofit 调用
     */
    suspend fun <T> safeApiCall(
        policy: ErrorPolicy = ErrorPolicy.Standard,
        block: suspend () -> Response<T>,
    ): ApiResult<T> {
        return try {
            handleRetrofitResponse(block(), policy)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleThrowable(e, policy)
        }
    }

    fun <T> handleRetrofitResponse(
        response: Response<T>,
        policy: ErrorPolicy = ErrorPolicy.Standard,
    ): ApiResult<T> {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                return ApiResult.Success(body)
            }
            if (response.code() == 204) {
                @Suppress("UNCHECKED_CAST")
                return ApiResult.Success(Unit as T)
            }
            return ApiResult.Error(response.code(), "响应数据为空")
        }
        return ApiResult.Error(
            code = response.code(),
            message = resolveErrorMessage(response, policy),
        )
    }

    private fun <T> resolveErrorMessage(
        response: Response<T>,
        policy: ErrorPolicy,
    ): String {
        parseErrorMessage(response)?.let { return it }
        return when (response.code()) {
            400 -> "无错误提示，请联系管理员"
            401 -> when (policy) {
                ErrorPolicy.Login -> "登录失败，请检查水司代码、用户名、密码或验证码"
                ErrorPolicy.Standard -> "登录已过期，请重新登录"
            }
            403 -> when (policy) {
                ErrorPolicy.Login -> "登录失败，请检查水司代码、用户名、密码或验证码"
                ErrorPolicy.Standard -> "登录已过期，请重新登录"
            }
            404 -> "未找到服务，请联系管理员"
            500 -> "服务异常"
            else -> "请求失败 (${response.code()})"
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String? {
        return runCatching { response.errorBody()?.string()?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun handleThrowable(
        throwable: Throwable,
        policy: ErrorPolicy = ErrorPolicy.Standard,
    ): ApiResult.Error {
        return when (throwable) {
            is SocketTimeoutException -> ApiResult.Error(
                message = "请求超时，请稍后重试",
                throwable = throwable,
            )
            is IOException -> ApiResult.Error(
                message = "网络连接失败，请检查网络",
                throwable = throwable,
            )
            is HttpException -> ApiResult.Error(
                code = throwable.code(),
                message = parseHttpExceptionMessage(throwable, policy),
                throwable = throwable,
            )
            is AppException -> ApiResult.Error(
                code = (throwable as? AppException.ServerException)?.code ?: -1,
                message = throwable.message ?: "请求失败",
                throwable = throwable,
            )
            else -> ApiResult.Error(
                message = throwable.message ?: "未知错误",
                throwable = throwable,
            )
        }
    }

    fun userMessage(throwable: Throwable): String = handleThrowable(throwable).message

    fun isExpectedNetworkFailure(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            when (current) {
                is SocketTimeoutException,
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketException,
                is java.io.InterruptedIOException,
                -> return true
            }
            if (current is IOException) {
                val msg = current.message.orEmpty()
                if (msg.contains("网络") ||
                    msg.contains("Failed to connect", ignoreCase = true) ||
                    msg.contains("Unable to resolve host", ignoreCase = true)
                ) {
                    return true
                }
            }
            current = current.cause
        }
        return false
    }

    private fun parseHttpExceptionMessage(
        throwable: HttpException,
        policy: ErrorPolicy,
    ): String {
        val body = runCatching { throwable.response()?.errorBody()?.string()?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (body != null) return body
        return when (throwable.code()) {
            400 -> "无错误提示，请联系管理员"
            401, 403 -> when (policy) {
                ErrorPolicy.Login -> "登录失败，请检查水司代码、用户名、密码或验证码"
                ErrorPolicy.Standard -> "登录已过期，请重新登录"
            }
            else -> "服务器错误 (${throwable.code()})"
        }
    }
}

fun createAppJson(): Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}
