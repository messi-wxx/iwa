package com.cq.iwa.feature.calibration.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CalibrationApi {

    @GET("edc/v1/ReadReplace/LocationMark/GetMeterLocationPath/{code}")
    suspend fun getLocationPath(@Path("code") code: String): Response<List<AddressResultDto>>

    @GET("edc/v1/ReadReplace/LocationMark/PlaceName/{parameter}")
    suspend fun searchPlace(
        @Path("parameter") parameter: String,
        @Query("PageSize") pageSize: Int,
        @Query("CurrentPage") currentPage: Int,
    ): Response<PlaceSearchDto>

    @GET("edc/v1/ReadReplace/LocationMark/getlocationPlaceInfo/{id}")
    suspend fun getLocationPlace(@Path("id") id: Long): Response<LocationPlaceDto>

    @GET("edc/v1/ReadReplace/LocationMark/LastRecord/{name}")
    suspend fun getMeterPlace(
        @Path("name") name: String,
        @Query("id") id: Long,
    ): Response<LocationPlaceDto>

    @GET("edc/v1/ReadReplace/LocationMark/RecordHistory/{name}")
    suspend fun queryHistoryRecords(
        @Path("name") name: String,
        @Query("id") id: Long,
        @Query("PageSize") pageSize: Int,
        @Query("CurrentPage") currentPage: Int,
    ): Response<List<LocationRecordDto>>

    @GET("edc/v1/ReadReplace/LocationMark/PlaceInfo/{id}")
    suspend fun queryRecordDetail(@Path("id") id: Int): Response<LocationPlaceDto>

    @POST("edc/v1/ReadReplace/LocationMark/postLocationPlace")
    suspend fun submitLocationPlace(@Body body: LocationPlaceSubmitDto): Response<ResponseBody>

    @POST("edc/v1/ReadReplace/LocationMark/PostMeterPlace")
    suspend fun submitMeterPlace(@Body body: LocationPlaceSubmitDto): Response<ResponseBody>
}
