package com.cq.iwa.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 刷新失败后通知 UI 跳转登录（401 且 Authenticator 无法续期）
 */
@Singleton
class AuthSessionManager @Inject constructor(
    private val sessionStore: SessionStore,
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun notifySessionExpired() {
        sessionStore.clearSession()
        _sessionExpired.tryEmit(Unit)
    }
}
