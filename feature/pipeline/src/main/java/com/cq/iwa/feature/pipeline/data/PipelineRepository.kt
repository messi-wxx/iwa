package com.cq.iwa.feature.pipeline.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.pipeline.network.PipelineAlarmRecordResultDto
import com.cq.iwa.feature.pipeline.network.PipelineApi
import com.cq.iwa.feature.pipeline.network.PipelineFollowDeviceDto
import com.cq.iwa.feature.pipeline.network.PipelineHistoryParam
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.network.PipelineMonitorDto
import com.cq.iwa.feature.pipeline.network.PipelineProfileDto
import com.cq.iwa.feature.pipeline.network.PipelineRecordParam
import com.cq.iwa.feature.pipeline.network.PipelineSiteInfoDto
import com.cq.iwa.feature.pipeline.network.PipelineTreeItemDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineRepository @Inject constructor(
    private val api: PipelineApi,
) {

    suspend fun getWebSocketUrl(): ApiResult<String> {
        return when (val result = ApiExceptionHandler.safeApiCall { api.getWebSocketUrl() }) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val url = result.data.string().trim()
                if (url.isEmpty()) ApiResult.Error(message = "未获取到实时通道地址") else ApiResult.Success(url)
            }
        }
    }

    suspend fun getFollowList(): ApiResult<List<PipelineFollowDeviceDto>> =
        ApiExceptionHandler.safeApiCall { api.getFollowList() }

    suspend fun getSiteTree(): ApiResult<List<PipelineTreeItemDto>> =
        ApiExceptionHandler.safeApiCall { api.getSiteTree(notShowDevice = false, addRootNode = false) }

    suspend fun getSiteInfo(id: Int): ApiResult<PipelineSiteInfoDto> =
        ApiExceptionHandler.safeApiCall { api.getSiteSketchById(id) }

    suspend fun getMetricList(siteId: Int): ApiResult<List<PipelineMetricDto>> =
        ApiExceptionHandler.safeApiCall { api.getMetricList(listOf(siteId)) }

    suspend fun getTimeseries(
        metric: PipelineMetricDto,
        startDate: String,
        endDate: String,
    ): ApiResult<List<PipelineMonitorDto>> {
        val result = ApiExceptionHandler.safeApiCall {
            api.getTimeseriesByDevice(
                PipelineHistoryParam(
                    deviceId = metric.sourceIotId,
                    beginTime = "${startDate}T00:00:00.000Z",
                    endTime = "${endDate}T23:59:59.999Z",
                    keys = listOf(metric.metricId),
                    deviceType = metric.sourceSiteType,
                ),
            )
        }
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val list = result.data[metric.metricId].orEmpty().map { item ->
                    item.copy(value = PipelineFormat.formatFloat(item.value, metric.digit))
                }
                ApiResult.Success(list)
            }
        }
    }

    suspend fun addFollow(siteId: Int, metricId: Int): ApiResult<Unit> =
        mutate { api.addFollow(mapOf("siteId" to siteId, "metricId" to metricId)) }

    suspend fun cancelFollow(siteId: Int, metricId: Int): ApiResult<Unit> =
        mutate { api.cancelFollow(mapOf("siteId" to siteId, "metricId" to metricId)) }

    suspend fun deleteSite(siteId: Int): ApiResult<Unit> =
        mutate { api.deleteSite(siteId) }

    suspend fun updateFollowOrder(items: List<PipelineFollowDeviceDto>): ApiResult<Unit> {
        val body = items.mapIndexed { index, item ->
            mapOf("siteId" to item.siteId, "sort" to index + 1)
        }
        return mutate { api.updateFollowOrder(body) }
    }

    suspend fun getAlarmRecords(
        param: PipelineRecordParam,
        pageSize: Int,
        currentPage: Int,
    ): ApiResult<PipelineAlarmRecordResultDto> =
        ApiExceptionHandler.safeApiCall { api.getAlarmRecords(param, pageSize, currentPage) }

    suspend fun getCredentials(iotId: String): String? {
        return runCatching {
            val response = api.getCredentialsByIotId(iotId)
            if (response.isSuccessful) response.body()?.string() else null
        }.getOrNull()
    }

    suspend fun getProfiles(siteType: Int): ApiResult<List<PipelineProfileDto>> =
        ApiExceptionHandler.safeApiCall {
            if (siteType == 1) api.getAssetProfiles() else api.getDeviceProfiles()
        }

    private suspend fun mutate(block: suspend () -> Response<okhttp3.ResponseBody>): ApiResult<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                when (val mapped = ApiExceptionHandler.handleRetrofitResponse(response)) {
                    is ApiResult.Error -> mapped
                    is ApiResult.Success -> ApiResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            ApiExceptionHandler.handleThrowable(e)
        }
    }
}
