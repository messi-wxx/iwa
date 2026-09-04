package com.cq.iwa.core.common.model

/**
 * 网络 / 业务层统一结果
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: Int = -1,
        val message: String,
        val throwable: Throwable? = null,
    ) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.data
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onError(block: (ApiResult.Error) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) block(this)
    return this
}
