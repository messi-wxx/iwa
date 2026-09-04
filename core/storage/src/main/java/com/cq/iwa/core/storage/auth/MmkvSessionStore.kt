package com.cq.iwa.core.storage.auth

import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.network.auth.TokenProvider
import com.cq.iwa.core.storage.mmkvDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmkvSessionStore @Inject constructor() : SessionStore {

    private var tokenCache by mmkvDelegate("auth_token", "")
    private var customerCodeCache by mmkvDelegate("auth_customer_code", "")
    private var userCodeCache by mmkvDelegate("auth_user_code", "")

    override fun getToken(): String? = tokenCache.takeIf { it.isNotBlank() }

    override fun getRawToken(): String? =
        getToken()?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotBlank() }

    override fun getCustomerCode(): String? = customerCodeCache.takeIf { it.isNotBlank() }

    override fun getUserCode(): String? = userCodeCache.takeIf { it.isNotBlank() }

    override fun saveToken(token: String) {
        tokenCache = token
    }

    override fun saveCustomerCode(customerCode: String) {
        customerCodeCache = customerCode
    }

    override fun saveUserCode(userCode: String) {
        userCodeCache = userCode
    }

    override fun clearToken() {
        tokenCache = ""
    }

    override fun clearSession() {
        tokenCache = ""
        customerCodeCache = ""
        userCodeCache = ""
    }
}
