package com.cq.iwa.feature.diagnose.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DiagnoseApi {

    @GET("epo/v1/Business/PartInfo/GetPartInfoByCode")
    suspend fun getPartInfoByCode(@Query("code") code: String): Response<DiagnosePartInfoDto>

    @GET("epo/v1/Business/ProductInfo/GetProductInfoByCode")
    suspend fun getProductInfoByCode(@Query("code") code: String): Response<DiagnoseProductDto>

    @POST("epo/v1/Business/ProductInfo/BatchUpdatePropertyInfo")
    suspend fun submitUpdateReading(@Body body: UpdateReadingRequestDto): Response<UpdateLogResultDto>
}
