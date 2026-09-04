package com.cq.iwa.core.network.auth

/**
 * 登录会话读写，供 Token 拦截器 / 刷新使用
 */
interface SessionStore : TokenProvider {

    fun getRawToken(): String?

    fun getCustomerCode(): String?

    fun getUserCode(): String?

    fun saveCustomerCode(customerCode: String)

    fun saveUserCode(userCode: String)

    fun clearSession()
}
