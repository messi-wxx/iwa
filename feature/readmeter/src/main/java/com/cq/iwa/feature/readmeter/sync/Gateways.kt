package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.readmeter.network.BookDto
import com.cq.iwa.feature.readmeter.network.ErrorMsgDto
import com.cq.iwa.feature.readmeter.network.MeterApi
import com.cq.iwa.feature.readmeter.network.MeterDto
import com.cq.iwa.feature.readmeter.network.MeterExtInfoDto
import com.cq.iwa.feature.readmeter.network.ReadModelDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdcGateway @Inject constructor(
    private val api: MeterApi,
) : MeterPlatformGateway {

    override suspend fun fetchBooks(readName: String): ApiResult<List<BookDto>> =
        ApiExceptionHandler.safeApiCall { api.getEdcBooks(readName) }

    override suspend fun fetchMeters(
        taskId: String,
        readName: String,
        meterType: Int,
    ): ApiResult<List<MeterDto>> =
        ApiExceptionHandler.safeApiCall { api.getEdcMeters(taskId, readName) }

    override suspend fun uploadReadings(
        taskId: String,
        models: List<ReadModelDto>,
    ): ApiResult<List<ErrorMsgDto>> = api.uploadEdc(models).asUnitSuccess()

    override suspend fun fetchFingerprint(taskId: String, meterType: Int): ApiResult<String?> =
        ApiResult.Success(null)
}

@Singleton
class ItWaterGateway @Inject constructor(
    private val api: MeterApi,
) : MeterPlatformGateway {

    override suspend fun fetchBooks(readName: String): ApiResult<List<BookDto>> =
        ApiExceptionHandler.safeApiCall { api.getItWaterBooks(readName) }

    override suspend fun fetchMeters(
        taskId: String,
        readName: String,
        meterType: Int,
    ): ApiResult<List<MeterDto>> =
        ApiExceptionHandler.safeApiCall { api.getItWaterMeters(taskId) }

    override suspend fun uploadReadings(
        taskId: String,
        models: List<ReadModelDto>,
    ): ApiResult<List<ErrorMsgDto>> = api.uploadItWater(models).asUnitSuccess()

    override suspend fun fetchFingerprint(taskId: String, meterType: Int): ApiResult<String?> =
        ApiResult.Success(null)

    override suspend fun fetchExtInfo(
        meterCodes: List<String>,
    ): ApiResult<List<MeterExtInfoDto>> =
        ApiExceptionHandler.safeApiCall { api.getItWaterExtInfo(meterCodes) }
}

@Singleton
class BcpGateway @Inject constructor(
    private val api: MeterApi,
) : MeterPlatformGateway {

    override suspend fun fetchBooks(readName: String): ApiResult<List<BookDto>> =
        ApiExceptionHandler.safeApiCall { api.getBcpBooks(readName) }

    override suspend fun fetchMeters(
        taskId: String,
        readName: String,
        meterType: Int,
    ): ApiResult<List<MeterDto>> =
        ApiExceptionHandler.safeApiCall { api.getBcpMeters(taskId, meterType) }

    override suspend fun uploadReadings(
        taskId: String,
        models: List<ReadModelDto>,
    ): ApiResult<List<ErrorMsgDto>> =
        ApiExceptionHandler.safeApiCall { api.uploadBcp(taskId, models) }

    override suspend fun fetchFingerprint(taskId: String, meterType: Int): ApiResult<String?> {
        return when (val result = ApiExceptionHandler.safeApiCall { api.getBcpHash(taskId, meterType) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.string().trim().ifBlank { null })
            is ApiResult.Error -> result
        }
    }
}

@Singleton
class GatewayFactory @Inject constructor(
    private val edc: EdcGateway,
    private val itWater: ItWaterGateway,
    private val bcp: BcpGateway,
) {
    fun of(platform: com.cq.iwa.feature.readmeter.MeterPlatform): MeterPlatformGateway {
        return when (platform) {
            com.cq.iwa.feature.readmeter.MeterPlatform.EDC -> edc
            com.cq.iwa.feature.readmeter.MeterPlatform.ITWATER -> itWater
            com.cq.iwa.feature.readmeter.MeterPlatform.BCP -> bcp
        }
    }
}
