package com.cq.iwa.feature.dma.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * DMA 分区接口。文档里的 customerCode 不传，由登录 token 在服务端解析。
 */
interface DmaApi {

    @GET("dma/v3/AppService/Subarea/LevelOptions")
    suspend fun levelOptions(): Response<List<DmaLevelOptionDto>>

    @GET("dma/v3/AppService/Subarea/Parent")
    suspend fun parent(): Response<List<DmaParentDto>>

    @GET("dma/v3/AppService/Subarea/List")
    suspend fun list(
        @Query("FilterText") filterText: String? = null,
        @Query("LevelNum") levelNum: Int? = null,
        @Query("ParentId") parentId: Int? = null,
        @Query("State") state: Int? = null,
        @Query("SortKind") sortKind: Int? = null,
        @Query("PageSize") pageSize: Int? = null,
        @Query("CurrentPage") currentPage: Int? = null,
        @Query("PageTotal") pageTotal: Int? = null,
        @Query("ExtraInfo") extraInfo: String? = null,
    ): Response<DmaListDto>

    @GET("dma/v3/AppService/Subarea/Detail")
    suspend fun detail(@Query("subareaId") subareaId: Long): Response<DmaDetailDto>

    @GET("dma/v3/AppService/Subarea/FlowTrend")
    suspend fun flowTrend(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
    ): Response<DmaTrendSeriesDto>

    @GET("dma/v3/AppService/Subarea/NightFlowTrend")
    suspend fun nightFlowTrend(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
    ): Response<List<DmaTrendSeriesDto>>

    @GET("dma/v3/AppService/Subarea/MinNightFlowTrend")
    suspend fun minNightFlowTrend(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
    ): Response<DmaTrendSeriesDto>

    @GET("dma/v3/AppService/Subarea/NightFlowAndKLRateTrend")
    suspend fun nightFlowAndKlRateTrend(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
    ): Response<List<DmaTrendSeriesDto>>

    @GET("dma/v3/AppService/Subarea/ELFlowTrend")
    suspend fun elFlowTrend(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
    ): Response<DmaTrendSeriesDto>

    @GET("dma/v3/AppService/Subarea/LeakageRateTrend")
    suspend fun leakageRateTrend(
        @Query("subareaId") subareaId: Long,
        @Query("year") year: Int,
    ): Response<List<DmaTrendSeriesDto>>

    @GET("dma/v3/AppService/Subarea/FlowDetail")
    suspend fun flowDetail(
        @Query("subareaId") subareaId: Long,
        @Query("beginDate") beginDate: String,
        @Query("endDate") endDate: String,
        @Query("PageSize") pageSize: Int? = null,
        @Query("CurrentPage") currentPage: Int? = null,
        @Query("PageTotal") pageTotal: Int? = null,
        @Query("ExtraInfo") extraInfo: String? = null,
    ): Response<DmaFlowDetailDto>

    @GET("dma/v3/AppService/Subarea/LeakageRateDetail")
    suspend fun leakageRateDetail(
        @Query("subareaId") subareaId: Long,
        @Query("year") year: Int,
    ): Response<List<DmaLeakageMonthDto>>

    @GET("dma/v3/AppService/Subarea/NonRevenueWaterDetail")
    suspend fun nonRevenueWaterDetail(
        @Query("subareaId") subareaId: Long,
        @Query("year") year: Int,
    ): Response<List<DmaNrwMonthDto>>

    @GET("dma/v3/AppService/Subarea/DeviceFlow")
    suspend fun deviceFlow(
        @Query("subareaId") subareaId: Long,
        @Query("readDate") readDate: String,
    ): Response<List<DmaDeviceFlowDto>>
}
