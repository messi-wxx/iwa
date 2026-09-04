package com.cq.iwa.feature.diagnose.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.diagnose.network.DiagnoseApi
import com.cq.iwa.feature.diagnose.network.DiagnosePartInfoDto
import com.cq.iwa.feature.diagnose.network.DiagnoseProductDto
import com.cq.iwa.feature.diagnose.network.UpdateLogResultDto
import com.cq.iwa.feature.diagnose.network.UpdateReadingRequestDto
import com.cq.iwa.feature.diagnose.network.UpdateWaterItemDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnoseRepository @Inject constructor(
    private val api: DiagnoseApi,
    private val authRepository: AuthRepository,
) {

    suspend fun canWriteWaterReading(): Boolean {
        val user = authRepository.getCurrentUser() ?: return false
        return authRepository.decodeMenus(user.menuJson)
            .any { menu -> menu.children.orEmpty().any { it.path == "writeWaterReading" } }
    }

    suspend fun canDemoWriteWater(): Boolean {
        val user = authRepository.getCurrentUser() ?: return false
        return user.code.equals("wxx", ignoreCase = true) &&
            user.customerCode.equals("demo", ignoreCase = true)
    }

    suspend fun getPartInfoByCode(code: String): ApiResult<DiagnosePartInfoDto> =
        ApiExceptionHandler.safeApiCall { api.getPartInfoByCode(code) }

    suspend fun getProductInfoByCode(code: String): ApiResult<DiagnoseProductDto> =
        ApiExceptionHandler.safeApiCall { api.getProductInfoByCode(code) }

    suspend fun submitUpdateReading(
        meterCode: String,
        initWater: Float,
        appVersion: String,
    ): ApiResult<UpdateLogResultDto> {
        val readTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
            .replace(" ", "T")
        val body = UpdateReadingRequestDto(
            items = listOf(
                UpdateWaterItemDto(
                    code = meterCode,
                    initWater = initWater,
                    readTime = readTime,
                ),
            ),
            appVersion = appVersion,
        )
        return ApiExceptionHandler.safeApiCall { api.submitUpdateReading(body) }
    }
}
