package com.cq.iwa.core.network.auth

/**
 * Token 存取抽象，由 storage 模块或 app 层实现
 */
interface TokenProvider {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}
