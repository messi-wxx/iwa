package com.cq.iwa.feature.pipeline.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PipelineApi {

    @GET("pmc/v1/BaseService/Base/GetThreePartyUrl")
    suspend fun getWebSocketUrl(@Query("urlType") urlType: Int = 1): Response<ResponseBody>

    @GET("pmc/v1/BaseService/Base/GetSiteTree")
    suspend fun getSiteTree(
        @Query("notShowDevice") notShowDevice: Boolean,
        @Query("addRootNode") addRootNode: Boolean,
    ): Response<List<PipelineTreeItemDto>>

    @GET("pmc/v1/ArchivesMonitor/Site/GetSiteSketchById")
    suspend fun getSiteSketchById(@Query("id") id: Int): Response<PipelineSiteInfoDto>

    @POST("pmc/v1/BaseService/Alarm/getSiteSortedMetricsBySiteIds")
    suspend fun getMetricList(
        @Body siteIds: List<Int>,
        @Query("LoadUnSortMetrics") loadUnSortMetrics: Boolean = true,
    ): Response<List<PipelineMetricDto>>

    @POST("pmc/v1/Summary/TimeSeries/GetTimeseriesByDevice")
    suspend fun getTimeseriesByDevice(
        @Body params: PipelineHistoryParam,
    ): Response<Map<String, List<PipelineMonitorDto>>>

    @POST("pmc/v1/BaseService/App/GetUserFocusSiteList")
    suspend fun getFollowList(): Response<List<PipelineFollowDeviceDto>>

    @POST("pmc/v1/BaseService/App/AddFocusMetric")
    suspend fun addFollow(@Body map: Map<String, Int>): Response<ResponseBody>

    @POST("pmc/v1/BaseService/App/DeleteFocusMetric")
    suspend fun cancelFollow(@Body map: Map<String, Int>): Response<ResponseBody>

    @POST("pmc/v1/BaseService/App/DeleteFocusSite")
    suspend fun deleteSite(@Query("siteId") id: Int): Response<ResponseBody>

    @POST("pmc/v1/BaseService/App/SortFocusSites")
    suspend fun updateFollowOrder(@Body body: List<Map<String, Int>>): Response<ResponseBody>

    @POST("pmc/v1/BaseService/Alarm/GetAlarmRecords")
    suspend fun getAlarmRecords(
        @Body body: PipelineRecordParam,
        @Query("PageSize") pageSize: Int,
        @Query("CurrentPage") currentPage: Int,
    ): Response<PipelineAlarmRecordResultDto>

    @GET("pmc/v1/BaseService/Base/GetCredentialsByIotId")
    suspend fun getCredentialsByIotId(@Query("iotId") iotId: String): Response<ResponseBody>

    @GET("pmc/v1/BaseService/Base/GetDeviceProfiles")
    suspend fun getDeviceProfiles(): Response<List<PipelineProfileDto>>

    @GET("pmc/v1/BaseService/Base/GetAssetProfiles")
    suspend fun getAssetProfiles(): Response<List<PipelineProfileDto>>
}
