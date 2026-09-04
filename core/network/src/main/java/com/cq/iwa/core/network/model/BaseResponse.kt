package com.cq.iwa.core.network.model

import kotlinx.serialization.Serializable

/**
 * 服务端统一响应包装
 */
@Serializable
data class BaseResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
) {
    val isSuccess: Boolean get() = code == 0 || code == 200
}
