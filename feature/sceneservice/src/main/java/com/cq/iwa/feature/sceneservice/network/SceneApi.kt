package com.cq.iwa.feature.sceneservice.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SceneApi {

    @GET("portal/v1/service/device")
    suspend fun queryDevice(
        @Query("code") code: String,
        @Query("targetCustomerCode") targetCustomerCode: String,
    ): Response<SceneQueryResultDto>

    @GET("portal/v1/basic/customer")
    suspend fun queryCustomer(
        @Query("name") name: String,
        @Query("moduleCode") moduleCode: String,
    ): Response<List<SceneCustomerDto>>

    @POST("edc/v1/Flux/ArtificialReading")
    suspend fun singleReadSave(@Body body: SceneSingleReadRequestDto): Response<Unit>

    @GET("edc/v1/DeviceArchives/Device/GetDeviceInnerInfoById")
    suspend fun getDeviceInfo(@Query("id") id: Long): Response<SceneDeviceInfoDto>

    @POST("edc/v1/DeviceArchives/Device/RePlaceDevice")
    suspend fun replaceDevice(@Body body: SceneDeviceInfoDto): Response<Unit>

    @GET("edc/v1/BasicSetting/DeviceTag/GetTagOption")
    suspend fun getDeviceTag(): Response<List<SceneDictOptionDto>>

    @GET("edc/v1/BasicSetting/DeviceNameplate/GetNameplateOption")
    suspend fun getDeviceNameplate(): Response<List<SceneNameplateOptionDto>>

    @GET("edc/v1/BasicSetting/DataDictionary/GetDictionaryOption")
    suspend fun getDictionary(@Query("Code") code: String): Response<List<SceneDictOptionDto>>

    @GET("edc/v1/BasicSetting/DeviceCaliber/GetCaliberOption")
    suspend fun getCaliber(): Response<List<SceneDictOptionDto>>

    @GET("edc/v1/BasicSetting/BookMeterWater/GetBookTreeOptions")
    suspend fun getBookTreeOptions(): Response<List<SceneBookDto>>

    @POST("epo/v1/Business/PartInfo/ChangeFullCode")
    suspend fun updatePartICode(@Body body: SceneICodeRequestDto): Response<Unit>

    @POST("epo/v1/Business/ProductInfo/ChangeFullCode")
    suspend fun updateProductICode(@Body body: SceneICodeRequestDto): Response<Unit>

    @GET("epo/v1/Business/ProductInfo/GetProductInfoById")
    suspend fun getProductInfoById(@Query("id") id: String): Response<SceneProductDto>

    @GET("epo/v1/Business/PartInfo/GetPartInfoByCode")
    suspend fun getPartInfoByCode(@Query("code") code: String): Response<SceneProductDto>

    @POST("epo/v1/BaseService/Define/GetProductDefineInfoByPartDefines")
    suspend fun getProductDefineByPartDefines(
        @Body body: ScenePartDefineIdsBodyDto,
    ): Response<SceneProductDefineDto>

    @GET("epo/v1/BaseService/Define/GetProductDefineInfoById")
    suspend fun getProductDefineById(
        @Query("id") id: String,
        @Query("productId") productId: String,
    ): Response<SceneProductDefineDto>

    @POST("epo/v1/Business/ReplaceDevice/ReplacePart")
    suspend fun replacePart(@Body body: SceneReplacePartBodyDto): Response<Unit>
}
