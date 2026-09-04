package com.cq.iwa.core.network.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 现场服务切换水司后的临时 Token，对齐老项目 `token_temp`。
 * 只作用于内存，不覆盖登录主 Token；查询页销毁或重新查询时清空。
 */
@Singleton
class SceneTempTokenStore @Inject constructor() {

    @Volatile
    private var token: String? = null

    fun getToken(): String? = token?.takeIf { it.isNotBlank() }

    fun hasToken(): Boolean = getToken() != null

    fun save(token: String) {
        this.token = token
    }

    fun clear() {
        token = null
    }
}
