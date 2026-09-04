package com.cq.iwa.feature.installation.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming

interface InstApi {

    @POST("imp/v1/Business/Workbench/WorkBenchList")
    suspend fun getWorkbenchList(
        @Query("pageSize") pageSize: Int,
        @Query("currentPage") currentPage: Int,
        @Body body: InstListRequest,
    ): Response<InstListResultDto>

    @POST("imp/v1/Business/Project/GetProjectInfoList")
    suspend fun getAllProjectList(
        @Query("pageSize") pageSize: Int,
        @Query("currentPage") currentPage: Int,
        @Body body: InstListRequest,
    ): Response<InstListResultDto>

    @POST("imp/v1/Business/Workbench/MyWorkBench")
    suspend fun getWorkbenchCount(): Response<InstQtyResultDto>

    @POST("imp/v1/Business/Workbench/GetProjectSketchById")
    suspend fun getProjectSketchById(@Query("id") id: Int): Response<InstSketchResultDto>

    @GET("imp/v1/BaseService/ProcessTask/GetTaskDetail")
    suspend fun getTaskDetail(@Query("taskId") taskId: String): Response<InstTaskDetailDto>

    @GET("imp/v1/BaseService/ProcessInstance/GetProcessInstanceDetail")
    suspend fun getProcessInstanceDetail(
        @Query("projectId") projectId: Int,
    ): Response<InstProcessDetailDto>

    @GET("imp/v1/BaseService/ProcessTask/GetRejectTargets")
    suspend fun getRejectTargets(@Query("taskId") taskId: String): Response<List<InstRejectTargetDto>>

    @POST("imp/v1/BaseService/ProcessTask/RejectTask")
    suspend fun rejectTask(@Body body: InstRejectBody): Response<ResponseBody>

    @POST("imp/v1/BaseService/ProcessTask/ExtendTask")
    suspend fun extendTask(@Body body: InstExtendBody): Response<ResponseBody>

    @POST("imp/v1/BaseService/ProcessTask/CompleteTask")
    suspend fun completeTask(@Body body: RequestBody): Response<ResponseBody>

    @POST("imp/v1/Business/Workbench/Follow")
    suspend fun followProject(@Query("projectId") projectId: Int): Response<ResponseBody>

    @POST("imp/v1/Business/Workbench/Urge")
    suspend fun urgeProject(@Body body: InstUrgeBody): Response<ResponseBody>

    @GET("imp/v1/BaseService/ProcessInstance/GetProcessOverview")
    suspend fun getProcessOverview(@Query("projectId") projectId: Int): Response<InstOverviewDto>

    @POST("imp/v1/BaseService/ProcessTask/ClaimTask")
    suspend fun claimTask(@Query("taskId") taskId: String): Response<ResponseBody>

    @GET("imp/v1/BaseService/DataDictionary/GetDictionaryOption")
    suspend fun getDictionaryOptionList(@Query("code") code: String): Response<ResponseBody>

    @GET("imp/v1/BaseService/Config/GetConfigOption")
    suspend fun getOptionList(@Query("code") code: String): Response<ResponseBody>

    @Streaming
    @POST("imp/v1/Business/Contract/ContractDown")
    suspend fun contractDown(@Query("projectId") projectId: Int): Response<ResponseBody>

    @Streaming
    @POST("imp/v1/Business/Contract/DocumentDown")
    suspend fun documentDown(@Body body: InstDocumentDownBody): Response<ResponseBody>

    @Multipart
    @POST("imp/v1/Business/Workbench/BatchMeterWaterInExcle")
    suspend fun uploadExcel(
        @Query("projectId") projectId: Int,
        @Part file: MultipartBody.Part,
    ): Response<ResponseBody>

    @POST("imp/v1/Business/Meter/GetMeterInstallInfoList")
    suspend fun getMeterInstallInfoList(
        @Query("pageSize") pageSize: Int = 999999,
        @Query("currentPage") currentPage: Int = 1,
        @Body body: InstProjectIdBody,
    ): Response<InstInstallResultDto>

    @POST("imp/v1/Business/Meter/GetMeterRecordInfoList")
    suspend fun getMeterRecordInfoList(
        @Query("pageSize") pageSize: Int = 9999,
        @Query("currentPage") currentPage: Int = 1,
        @Body body: InstRecordInfoBody,
    ): Response<InstMeterRecordResultDto>

    @POST("imp/v1/Business/Meter/PostMeterRecordInstall")
    suspend fun postMeterRecordInstall(@Body body: InstAddMeterBody): Response<ResponseBody>

    @POST("imp/v1/Business/Meter/PutMeterRecordInfo")
    suspend fun updateMeterRecordInfo(@Body body: InstAddMeterBody): Response<ResponseBody>

    @GET("imp/v1/Business/Meter/GetMeterRecordById")
    suspend fun getMeterRecordById(@Query("id") id: Int): Response<InstAddMeterBody>

    @DELETE("imp/v1/Business/Meter/Delete")
    suspend fun deleteMeterRecordById(@Query("id") id: Int): Response<ResponseBody>

    @POST("imp/v1/Business/ProjectHistory/GetList")
    suspend fun getProjectLogList(
        @Query("pageSize") pageSize: Int = 1000,
        @Query("currentPage") currentPage: Int = 1,
        @Body body: InstLogRequest,
    ): Response<InstLogResultDto>
}
