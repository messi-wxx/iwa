package com.cq.iwa.feature.urgepayment.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UrgePaymentApi {

    @GET("bcp/v1/Flux/BookMeterWater/GetBookMeterWaterForUrgeUser")
    suspend fun getUrgeTasks(): Response<List<UrgeTaskDto>>

    @GET("bcp/v1/Flux/BookMeterWater/GetAllMeterForBookId")
    suspend fun getMetersForBook(@Query("bookId") bookId: Int): Response<List<UrgeMeterDto>>

    @GET("bcp/v1/archive/Client/UrgeSearchClient")
    suspend fun searchClients(
        @Query("keyWord") keyWord: String,
        @Query("currentPage") currentPage: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<UrgeClientSearchDto>

    @GET("bcp/v1/archive/Client/DetailByCode")
    suspend fun getDetailByCode(@Query("code") code: String): Response<UrgeFeeClientDto>

    @GET("bcp/v1/FeeBusiness/Fee/GetNopayFeeForClientId")
    suspend fun getNopayFees(
        @Query("clientId") clientId: Int,
        @Query("isRounding") isRounding: Boolean = true,
        @Query("includeModuleStr") includeModuleStr: String = "",
    ): Response<List<UrgeFeeDetailDto>>

    @GET("bcp/v1/Flux/Flux/GetFluxByMeterId")
    suspend fun getReadHistory(
        @Query("MeterId") meterId: Int,
        @Query("PageSize") pageSize: Int,
        @Query("CurrentPage") currentPage: Int,
    ): Response<UrgeReadListDto>

    @GET("bcp/v1/archive/Client/NextSortClient/{id}")
    suspend fun nextSortClient(@Path("id") clientId: Int): Response<UrgeSortClientDto>

    @GET("bcp/v1/archive/Client/PreviousSortClient/{id}")
    suspend fun previousSortClient(@Path("id") clientId: Int): Response<UrgeSortClientDto>

    @PUT("bcp/v1/archive/Client/ChangePhone/{code}")
    suspend fun changePhone(
        @Path("code") code: String,
        @Body body: ChangePhoneBodyDto,
    ): Response<ResponseBody>

    @PUT("bcp/v1/archive/MeterWater/ChangeReaderRemark/{id}")
    suspend fun changeRemark(
        @Path("id") id: Int,
        @Body describe: String,
    ): Response<ResponseBody>

    @POST("bcp/v1/customerservice/ValveDetailed/OpenValve")
    suspend fun openValve(@Query("id") id: Long): Response<ResponseBody>

    @POST("bcp/v1/customerservice/ValveDetailed/CloseValve")
    suspend fun closeValve(@Query("id") id: Long): Response<ResponseBody>
}
