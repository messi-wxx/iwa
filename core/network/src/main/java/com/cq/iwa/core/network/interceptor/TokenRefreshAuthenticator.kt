package com.cq.iwa.core.network.interceptor

import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.network.api.PortalApi
import com.cq.iwa.core.network.auth.AuthSessionManager
import com.cq.iwa.core.network.auth.SceneTempTokenStore
import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.network.model.VerificationRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Token 过期时自动刷新，避免直接跳转登录（对齐 WMService portal/v1/verification）
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    @Named("portalPlainApi") private val portalApi: PortalApi,
    private val sessionStore: SessionStore,
    private val sceneTempTokenStore: SceneTempTokenStore,
    private val authSessionManager: AuthSessionManager,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        return try {
            authenticateOrNull(route, response)
        } catch (_: Exception) {
            null
        }
    }

    private fun authenticateOrNull(@Suppress("UNUSED_PARAMETER") route: Route?, response: Response): Request? {
        if (response.code != 401 && response.code != 403) return null
        if (sceneTempTokenStore.hasToken()) {
            return null
        }
        if (responseCount(response) >= 2) {
            authSessionManager.notifySessionExpired()
            return null
        }

        val rawToken = sessionStore.getRawToken()
        if (rawToken.isNullOrBlank()) {
            authSessionManager.notifySessionExpired()
            return null
        }
        val customerCode = sessionStore.getCustomerCode().orEmpty()

        val refreshResult = runBlocking {
            val request = VerificationRequest(token = rawToken, customerCode = customerCode)
            if (customerCode.isNotBlank()) {
                ApiExceptionHandler.safeApiCall { portalApi.refreshToken(request) }
            } else {
                ApiExceptionHandler.safeApiCall { portalApi.refreshTokenSimple(request) }
            }
        }
        val newToken = when (refreshResult) {
            is com.cq.iwa.core.common.model.ApiResult.Success -> refreshResult.data.token
            is com.cq.iwa.core.common.model.ApiResult.Error -> {
                if (isNetworkRefreshFailure(refreshResult)) return null
                null
            }
        }
        if (newToken.isNullOrBlank()) {
            authSessionManager.notifySessionExpired()
            return null
        }

        sessionStore.saveToken(newToken)
        val authorization = if (newToken.startsWith("Bearer ", ignoreCase = true)) {
            newToken
        } else {
            "Bearer $newToken"
        }
        return response.request.newBuilder()
            .header("Authorization", authorization)
            .build()
    }

    private fun isNetworkRefreshFailure(
        error: com.cq.iwa.core.common.model.ApiResult.Error,
    ): Boolean {
        val throwable = error.throwable
        if (throwable != null && ApiExceptionHandler.isExpectedNetworkFailure(throwable)) {
            return true
        }
        val message = error.message
        return message.contains("网络") || message.contains("超时")
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
