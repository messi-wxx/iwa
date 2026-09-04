package com.cq.iwa.feature.readmeter.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MeterApi {

    @GET("edc/v1/ReadReplace/MeterReading/GetAllTaskForReadName")
    suspend fun getEdcBooks(@Query("readName") readName: String): Response<List<BookDto>>

    @GET("metercrm/MeterReading/GetAllTaskForReadName")
    suspend fun getItWaterBooks(@Query("readName") readName: String): Response<List<BookDto>>

    @GET("bcp/v1/Flux/BookMeterWater/GetAllTaskForReadName")
    suspend fun getBcpBooks(@Query("readName") readName: String): Response<List<BookDto>>

    @GET("edc/v1/ReadReplace/MeterReading/GetAllTaskDetailForTaskId")
    suspend fun getEdcMeters(
        @Query("taskId") taskId: String,
        @Query("readName") readName: String,
    ): Response<List<MeterDto>>

    @GET("metercrm/MeterReading/GetAllTaskDetailForMonth")
    suspend fun getItWaterMeters(@Query("taskId") taskId: String): Response<List<MeterDto>>

    @GET("bcp/v1/Flux/BookMeterWater/GetAllTaskDetailForMonth")
    suspend fun getBcpMeters(
        @Query("taskId") taskId: String,
        @Query("meterType") meterType: Int,
        @Query("getAll") getAll: Boolean = true,
    ): Response<List<MeterDto>>

    @POST("edc/v1/ReadReplace/LocationMark/PostReplaceMeterPlace")
    suspend fun uploadLocations(@Body locations: List<MeterLocationDto>): Response<Unit>

    @POST("edc/v1/ReadReplace/MeterReading/MobileUploadFlux")
    suspend fun uploadEdc(@Body models: List<ReadModelDto>): Response<Unit>

    @POST("metercrm/MeterReading/MobileUploadFlux")
    suspend fun uploadItWater(@Body models: List<ReadModelDto>): Response<Unit>

    @POST("metercrm/MeterReading/GetExtInfForMeterCode?prefix=1")
    suspend fun getItWaterExtInfo(@Body meterCodes: List<String>): Response<List<MeterExtInfoDto>>

    @POST("bcp/v1/Flux/Flux/MobileUploadFlux")
    suspend fun uploadBcp(
        @Query("taskId") taskId: String,
        @Body models: List<ReadModelDto>,
    ): Response<List<ErrorMsgDto>>

    @GET("bcp/v1/Flux/BookMeterWater/GetHashForTaskMeterCodes")
    suspend fun getBcpHash(
        @Query("taskId") taskId: String,
        @Query("meterType") meterType: Int,
    ): Response<ResponseBody>

    @PUT("bcp/v1/archive/Client/ChangePhone/{code}")
    suspend fun changeContact(
        @Path("code") code: String,
        @Body map: @JvmSuppressWildcards Map<String, String>,
    ): Response<Unit>

    @PUT("bcp/v1/archive/MeterWater/ChangeReaderRemark/{id}")
    suspend fun changeDescribe(
        @Path("id") id: Int,
        @Body describe: String,
    ): Response<Unit>
}

interface FileApi {

    @Multipart
    @POST("api/file/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileDto>

    @GET("attachment/apk/{name}")
    suspend fun getVersionInfo(@Path("name") name: String): Response<VersionDto>
}
