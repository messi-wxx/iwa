package com.cq.iwa.feature.dma.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.dma.network.DmaApi
import com.cq.iwa.feature.dma.network.DmaDetailDto
import com.cq.iwa.feature.dma.network.DmaDeviceFlowDto
import com.cq.iwa.feature.dma.network.DmaFlowDetailDto
import com.cq.iwa.feature.dma.network.DmaLeakageMonthDto
import com.cq.iwa.feature.dma.network.DmaLevelOptionDto
import com.cq.iwa.feature.dma.network.DmaListDto
import com.cq.iwa.feature.dma.network.DmaNrwMonthDto
import com.cq.iwa.feature.dma.network.DmaParentDto
import com.cq.iwa.feature.dma.network.DmaTrendSeriesDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DmaRepository @Inject constructor(
    private val api: DmaApi,
) {

    suspend fun levelOptions(): ApiResult<List<DmaLevelOptionDto>> =
        ApiExceptionHandler.safeApiCall { api.levelOptions() }

    suspend fun parent(): ApiResult<List<DmaParentDto>> =
        ApiExceptionHandler.safeApiCall { api.parent() }

    suspend fun list(
        filterText: String? = null,
        levelNum: Int? = null,
        parentId: Int? = null,
        state: Int? = null,
        sortKind: Int? = null,
        pageSize: Int? = null,
        currentPage: Int? = null,
        pageTotal: Int? = null,
        extraInfo: String? = null,
    ): ApiResult<DmaListDto> = ApiExceptionHandler.safeApiCall {
        api.list(filterText, levelNum, parentId, state, sortKind, pageSize, currentPage, pageTotal, extraInfo)
    }

    suspend fun detail(subareaId: Long): ApiResult<DmaDetailDto> =
        ApiExceptionHandler.safeApiCall { api.detail(subareaId) }

    suspend fun flowTrend(subareaId: Long, beginDate: String, endDate: String): ApiResult<DmaTrendSeriesDto> =
        ApiExceptionHandler.safeApiCall { api.flowTrend(subareaId, beginDate, endDate) }

    suspend fun nightFlowTrend(subareaId: Long, beginDate: String, endDate: String): ApiResult<List<DmaTrendSeriesDto>> =
        ApiExceptionHandler.safeApiCall { api.nightFlowTrend(subareaId, beginDate, endDate) }

    suspend fun minNightFlowTrend(subareaId: Long, beginDate: String, endDate: String): ApiResult<DmaTrendSeriesDto> =
        ApiExceptionHandler.safeApiCall { api.minNightFlowTrend(subareaId, beginDate, endDate) }

    suspend fun nightFlowAndKlRateTrend(subareaId: Long, beginDate: String, endDate: String): ApiResult<List<DmaTrendSeriesDto>> =
        ApiExceptionHandler.safeApiCall { api.nightFlowAndKlRateTrend(subareaId, beginDate, endDate) }

    suspend fun elFlowTrend(subareaId: Long, beginDate: String, endDate: String): ApiResult<DmaTrendSeriesDto> =
        ApiExceptionHandler.safeApiCall { api.elFlowTrend(subareaId, beginDate, endDate) }

    suspend fun leakageRateTrend(subareaId: Long, year: Int): ApiResult<List<DmaTrendSeriesDto>> =
        ApiExceptionHandler.safeApiCall { api.leakageRateTrend(subareaId, year) }

    suspend fun flowDetail(
        subareaId: Long,
        beginDate: String,
        endDate: String,
        pageSize: Int? = null,
        currentPage: Int? = null,
        pageTotal: Int? = null,
        extraInfo: String? = null,
    ): ApiResult<DmaFlowDetailDto> = ApiExceptionHandler.safeApiCall {
        api.flowDetail(subareaId, beginDate, endDate, pageSize, currentPage, pageTotal, extraInfo)
    }

    suspend fun leakageRateDetail(subareaId: Long, year: Int): ApiResult<List<DmaLeakageMonthDto>> =
        ApiExceptionHandler.safeApiCall { api.leakageRateDetail(subareaId, year) }

    suspend fun nonRevenueWaterDetail(subareaId: Long, year: Int): ApiResult<List<DmaNrwMonthDto>> =
        ApiExceptionHandler.safeApiCall { api.nonRevenueWaterDetail(subareaId, year) }

    suspend fun deviceFlow(subareaId: Long, readDate: String): ApiResult<List<DmaDeviceFlowDto>> =
        ApiExceptionHandler.safeApiCall { api.deviceFlow(subareaId, readDate) }
}
