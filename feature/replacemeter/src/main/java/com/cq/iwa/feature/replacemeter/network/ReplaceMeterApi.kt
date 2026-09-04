package com.cq.iwa.feature.replacemeter.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ReplaceMeterApi {

    @GET("edc/v1/ReadReplace/ReplaceMeter/GetAllReplaceTaskForReplaceName")
    suspend fun getTasks(@Query("replaceName") replaceName: String): Response<List<ReplaceBookDto>>

    @GET("edc/v1/ReadReplace/ReplaceMeter/GetAllReplaceTaskDetailForTaskId")
    suspend fun getMeters(
        @Query("taskId") taskId: String,
        @Query("replaceName") replaceName: String,
    ): Response<List<ReplaceMeterDto>>

    @POST("edc/v1/ReadReplace/ReplaceMeter/UpLoadReplaceDetail")
    suspend fun upload(@Body models: List<ReplaceModelDto>): Response<Unit>

    @POST("edc/v1/ReadReplace/LocationMark/PostReplaceMeterPlace")
    suspend fun uploadLocations(@Body locations: List<ReplaceLocationDto>): Response<Unit>
}
