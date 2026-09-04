package com.cq.iwa.core.network.interceptor

import com.cq.iwa.core.network.auth.SceneTempTokenStore
import com.cq.iwa.core.network.auth.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
    private val sceneTempTokenStore: SceneTempTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sceneTempTokenStore.getToken() ?: sessionStore.getToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", formatAuthorization(token))
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }

    private fun formatAuthorization(token: String): String =
        if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
}
