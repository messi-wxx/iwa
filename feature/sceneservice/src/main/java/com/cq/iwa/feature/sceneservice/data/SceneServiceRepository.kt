package com.cq.iwa.feature.sceneservice.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.network.api.PortalApi
import com.cq.iwa.core.network.auth.SceneTempTokenStore
import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.network.model.VerificationRequest
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.sceneservice.network.SceneApi
import com.cq.iwa.feature.sceneservice.network.SceneBookDto
import com.cq.iwa.feature.sceneservice.network.SceneCustomerDto
import com.cq.iwa.feature.sceneservice.network.SceneDeviceInfoDto
import com.cq.iwa.feature.sceneservice.network.SceneDictOptionDto
import com.cq.iwa.feature.sceneservice.network.SceneICodeRequestDto
import com.cq.iwa.feature.sceneservice.network.SceneNameplateOptionDto
import com.cq.iwa.feature.sceneservice.network.ScenePartDefineIdsBodyDto
import com.cq.iwa.feature.sceneservice.network.SceneProductDefineDto
import com.cq.iwa.feature.sceneservice.network.SceneProductDto
import com.cq.iwa.feature.sceneservice.network.SceneQueryResultDto
import com.cq.iwa.feature.sceneservice.network.SceneReplacePartBodyDto
import com.cq.iwa.feature.sceneservice.network.SceneSingleReadRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneServiceRepository @Inject constructor(
    private val api: SceneApi,
    private val portalApi: PortalApi,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val sceneTempTokenStore: SceneTempTokenStore,
) {

    suspend fun isAqlCompany(): Boolean {
        val user = authRepository.getCurrentUser() ?: return false
        return user.customerCode.equals("aql", ignoreCase = true)
    }

    fun clearTempToken() {
        sceneTempTokenStore.clear()
    }

    suspend fun queryDevice(code: String, targetCustomerCode: String): ApiResult<SceneQueryResultDto> {
        sceneTempTokenStore.clear()
        return ApiExceptionHandler.safeApiCall { api.queryDevice(code, targetCustomerCode) }
    }

    suspend fun queryCustomers(name: String): ApiResult<List<SceneCustomerDto>> =
        ApiExceptionHandler.safeApiCall { api.queryCustomer(name, "edc,itwater") }

    suspend fun switchCompanyToken(customerCode: String): ApiResult<Unit> {
        val raw = sessionStore.getRawToken()
        if (raw.isNullOrBlank()) {
            return ApiResult.Error(message = "请重新登录")
        }
        val result = ApiExceptionHandler.safeApiCall {
            portalApi.refreshToken(
                VerificationRequest(
                    moduleCode = "Portal",
                    token = raw,
                    customerCode = customerCode,
                ),
            )
        }
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val token = result.data.token
                if (token.isBlank()) {
                    ApiResult.Error(message = "切换token错误")
                } else {
                    val header = if (token.startsWith("Bearer ", ignoreCase = true)) {
                        token
                    } else {
                        "Bearer $token"
                    }
                    sceneTempTokenStore.save(header)
                    ApiResult.Success(Unit)
                }
            }
        }
    }

    suspend fun singleReadSave(body: SceneSingleReadRequestDto): ApiResult<Unit> =
        ApiExceptionHandler.safeApiCall { api.singleReadSave(body) }

    suspend fun getDeviceInfo(id: Long): ApiResult<SceneDeviceInfoDto> =
        ApiExceptionHandler.safeApiCall { api.getDeviceInfo(id) }

    suspend fun replaceDevice(body: SceneDeviceInfoDto): ApiResult<Unit> =
        ApiExceptionHandler.safeApiCall { api.replaceDevice(body) }

    suspend fun getDeviceTag(): ApiResult<List<SceneDictOptionDto>> =
        ApiExceptionHandler.safeApiCall { api.getDeviceTag() }

    suspend fun getDeviceNameplate(): ApiResult<List<SceneNameplateOptionDto>> =
        ApiExceptionHandler.safeApiCall { api.getDeviceNameplate() }

    suspend fun getDictionary(code: String): ApiResult<List<SceneDictOptionDto>> =
        ApiExceptionHandler.safeApiCall { api.getDictionary(code) }

    suspend fun getCaliber(): ApiResult<List<SceneDictOptionDto>> =
        ApiExceptionHandler.safeApiCall { api.getCaliber() }

    suspend fun getBookTree(): ApiResult<List<SceneBookDto>> =
        ApiExceptionHandler.safeApiCall { api.getBookTreeOptions() }

    suspend fun updateProductICode(deviceId: String, fullCode: String): ApiResult<Unit> =
        ApiExceptionHandler.safeApiCall {
            api.updateProductICode(SceneICodeRequestDto(deviceID = deviceId, fullCode = fullCode))
        }

    suspend fun updatePartICode(deviceId: String, fullCode: String): ApiResult<Unit> =
        ApiExceptionHandler.safeApiCall {
            api.updatePartICode(SceneICodeRequestDto(deviceID = deviceId, fullCode = fullCode))
        }

    suspend fun getProductInfoById(id: String): ApiResult<SceneProductDto> =
        ApiExceptionHandler.safeApiCall { api.getProductInfoById(id) }

    suspend fun getPartInfoByCode(code: String): ApiResult<SceneProductDto> =
        ApiExceptionHandler.safeApiCall { api.getPartInfoByCode(code) }

    suspend fun getProductDefineByPartDefines(partDefineIds: List<String>): ApiResult<SceneProductDefineDto> =
        ApiExceptionHandler.safeApiCall {
            api.getProductDefineByPartDefines(ScenePartDefineIdsBodyDto(partDefineIds))
        }

    suspend fun getProductDefineById(id: String, productId: String): ApiResult<SceneProductDefineDto> =
        ApiExceptionHandler.safeApiCall { api.getProductDefineById(id, productId) }

    suspend fun replacePart(body: SceneReplacePartBodyDto): ApiResult<Unit> =
        ApiExceptionHandler.safeApiCall { api.replacePart(body) }
}
