package com.cq.iwa.feature.auth.repository

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.database.dao.UserConfigDao
import com.cq.iwa.core.database.dao.UserDao
import com.cq.iwa.core.database.entity.UserConfigEntity
import com.cq.iwa.core.database.entity.UserEntity
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.network.api.PortalApi
import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.network.model.CaptchaDto
import com.cq.iwa.core.network.model.LoginRequest
import com.cq.iwa.core.network.model.LoginUserDto
import com.cq.iwa.core.network.model.MenuDto
import com.cq.iwa.core.network.model.ResetPasswordRequest
import com.cq.iwa.core.network.model.UserConfigDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @Named("portalPlainApi") private val portalPlainApi: PortalApi,
    private val portalApi: PortalApi,
    private val userDao: UserDao,
    private val userConfigDao: UserConfigDao,
    private val sessionStore: SessionStore,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** 本次登录接口返回的首次登录，只活在当前进程，不写入本地用户。 */
    private var pendingFirstLogin: PendingFirstLogin? = null

    suspend fun getCaptcha(): ApiResult<CaptchaDto> = withContext(ioDispatcher) {
        ApiExceptionHandler.safeApiCall { portalPlainApi.getCaptcha() }
    }

    suspend fun login(request: LoginRequest): ApiResult<LoginUserDto> = withContext(ioDispatcher) {
        ApiExceptionHandler.safeApiCall(ApiExceptionHandler.ErrorPolicy.Login) {
            portalPlainApi.login(request)
        }
    }

    suspend fun loadAppConfig(): ApiResult<List<UserConfigDto>> = withContext(ioDispatcher) {
        ApiExceptionHandler.safeApiCall { portalApi.getAppConfig() }
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(ioDispatcher) {
        userDao.getCurrentUser()
    }

    suspend fun persistLogin(
        dto: LoginUserDto,
        customerCode: String,
        password: String,
    ) = withContext(ioDispatcher) {
        userDao.clearCurrentUserFlag()
        val entity = UserEntity(
            state = dto.state,
            name = dto.name,
            code = dto.code,
            token = dto.token,
            customer = dto.customer,
            customerCode = customerCode,
            password = password,
            menuJson = dto.menu?.let { json.encodeToString(it) },
            currentUser = true,
        )
        val existing = userDao.findByAccount(customerCode, dto.code)
        if (existing == null) {
            userDao.insert(entity)
        } else {
            userDao.update(entity.copy(id = existing.id))
        }
        sessionStore.saveToken(dto.token)
        sessionStore.saveCustomerCode(customerCode)
        sessionStore.saveUserCode(dto.code)
    }

    /**
     * 登录接口返回首次登录时，只把令牌放到当前进程，供改密接口使用。
     * 改密成功前不写入当前用户，进程结束后启动页会回到登录页。
     */
    suspend fun stageFirstLogin(
        dto: LoginUserDto,
        customerCode: String,
    ) = withContext(ioDispatcher) {
        pendingFirstLogin = PendingFirstLogin(dto, customerCode)
        sessionStore.saveToken(dto.token)
        sessionStore.saveCustomerCode(customerCode)
        sessionStore.saveUserCode(dto.code)
    }

    suspend fun hasPendingFirstLogin(): Boolean = withContext(ioDispatcher) {
        pendingFirstLogin != null
    }

    suspend fun pendingUserCode(): String? = withContext(ioDispatcher) {
        pendingFirstLogin?.user?.code?.takeIf { it.isNotBlank() }
    }

    suspend fun abandonFirstLogin() = withContext(ioDispatcher) {
        if (pendingFirstLogin == null) return@withContext
        pendingFirstLogin = null
        val user = userDao.getCurrentUser()
        if (user != null && user.state == 1) {
            sessionStore.saveToken(user.token)
            sessionStore.saveCustomerCode(user.customerCode)
            sessionStore.saveUserCode(user.code)
        } else {
            if (user != null) {
                userDao.clearCurrentUserFlag()
            }
            sessionStore.clearSession()
        }
    }

    suspend fun completeFirstLogin(newPassword: String) = withContext(ioDispatcher) {
        val pending = pendingFirstLogin ?: return@withContext
        persistLogin(pending.user.copy(state = 1), pending.customerCode, newPassword)
        pendingFirstLogin = null
    }

    suspend fun saveUserConfigs(configs: List<UserConfigDto>) = withContext(ioDispatcher) {
        userConfigDao.clearUserConfigs()
        val entities = configs.map {
            UserConfigEntity(
                id = it.id,
                customerId = it.customerId,
                kind = it.kind,
                configName = it.configName,
                configValue = it.configValue,
                seq = it.seq,
                description = it.description,
            )
        }.toMutableList()

        if (entities.none { it.configName == "autoNext" }) {
            entities.add(UserConfigEntity(0, "", "", "autoNext", "yes"))
        }
        if (entities.none { it.configName == "calculateReadingQty" }) {
            entities.add(UserConfigEntity(1, "", "", "calculateReadingQty", "yes"))
        }
        if (entities.none { it.configName == "forceNfcReading" }) {
            entities.add(UserConfigEntity(2, "", "", "forceNfcReading", "no"))
        }
        userConfigDao.insertAll(entities)
    }

    /** 冷启动时没有当前用户，清掉首次登录暂存的令牌。 */
    suspend fun clearDanglingSession() = withContext(ioDispatcher) {
        pendingFirstLogin = null
        if (userDao.getCurrentUser() == null) {
            sessionStore.clearSession()
        }
    }

    suspend fun restoreSessionFromDb() = withContext(ioDispatcher) {
        val user = userDao.getCurrentUser() ?: return@withContext null
        sessionStore.saveToken(user.token)
        sessionStore.saveCustomerCode(user.customerCode)
        sessionStore.saveUserCode(user.code)
        user
    }

    suspend fun logout() = withContext(ioDispatcher) {
        userDao.clearCurrentUserFlag()
        sessionStore.clearSession()
    }

    suspend fun resetPassword(
        code: String,
        oldPassword: String,
        password: String,
        confirmPassword: String,
    ): ApiResult<Unit> = withContext(ioDispatcher) {
        ApiExceptionHandler.safeApiCall {
            portalApi.resetPassword(
                ResetPasswordRequest(
                    code = code,
                    oldPassword = oldPassword,
                    password = password,
                    confirmPassword = confirmPassword,
                ),
            )
        }
    }

    suspend fun markPasswordChanged(newPassword: String) = withContext(ioDispatcher) {
        val user = userDao.getCurrentUser() ?: return@withContext
        userDao.updateStateAndPassword(user.code, state = 1, password = newPassword)
    }

    suspend fun isAutoNextEnabled(): Boolean = withContext(ioDispatcher) {
        val config = userConfigDao.findByName("autoNext")
        config?.configValue.equals("yes", ignoreCase = true)
    }

    suspend fun setAutoNextEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        val value = if (enabled) "yes" else "no"
        userConfigDao.updateValue("autoNext", value)
    }

    private data class PendingFirstLogin(
        val user: LoginUserDto,
        val customerCode: String,
    )

    fun decodeMenus(menuJson: String?): List<MenuDto> {
        if (menuJson.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(MenuDto.serializer()), menuJson)
        }.getOrDefault(emptyList())
    }
}
