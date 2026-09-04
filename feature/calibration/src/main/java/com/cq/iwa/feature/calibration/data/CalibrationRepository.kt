package com.cq.iwa.feature.calibration.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.calibration.network.AddressResultDto
import com.cq.iwa.feature.calibration.network.AttachmentDto
import com.cq.iwa.feature.calibration.network.CalibrationApi
import com.cq.iwa.feature.calibration.network.LocationPlaceDto
import com.cq.iwa.feature.calibration.network.LocationPlaceSubmitDto
import com.cq.iwa.feature.calibration.network.LocationRecordDto
import com.cq.iwa.feature.calibration.network.PlaceSearchDto
import com.cq.iwa.feature.readmeter.data.MeterLocalStore
import com.cq.iwa.feature.readmeter.data.MeterRepository
import com.cq.iwa.feature.readmeter.sync.PhotoUploader
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepository @Inject constructor(
    private val api: CalibrationApi,
    private val photoUploader: PhotoUploader,
    private val meterRepository: MeterRepository,
    private val replaceMeterRepository: ReplaceMeterRepository,
    private val localStore: MeterLocalStore,
    private val authRepository: AuthRepository,
) {

    suspend fun searchPlace(keyword: String, page: Int, pageSize: Int = 15): ApiResult<PlaceSearchDto> =
        ApiExceptionHandler.safeApiCall { api.searchPlace(keyword, pageSize, page) }

    suspend fun getLocationPath(code: String): ApiResult<List<AddressResultDto>> =
        ApiExceptionHandler.safeApiCall { api.getLocationPath(code) }

    suspend fun getLocationPlace(id: Long): ApiResult<LocationPlaceDto?> =
        nullableCall { api.getLocationPlace(id) }

    suspend fun getMeterPlace(name: String, id: Long): ApiResult<LocationPlaceDto?> =
        nullableCall { api.getMeterPlace(name, id) }

    suspend fun queryHistory(name: String, id: Long): ApiResult<List<LocationRecordDto>> =
        ApiExceptionHandler.safeApiCall { api.queryHistoryRecords(name, id, 100, 1) }

    suspend fun queryRecordDetail(id: Int): ApiResult<LocationPlaceDto?> =
        nullableCall { api.queryRecordDetail(id) }

    suspend fun createPictureFile(): File {
        val user = authRepository.getCurrentUser() ?: error("未登录")
        return localStore.createPictureFile(user.customerCode, user.code)
    }

    suspend fun submitPlace(
        isMeter: Boolean,
        body: LocationPlaceSubmitDto,
        photoPaths: List<String>,
        meterCode: String?,
    ): ApiResult<Unit> {
        val guids = photoUploader.uploadAll(photoPaths).getOrElse {
            return ApiResult.Error(-1, it.message ?: "数据提交失败-图片上传失败", it)
        }
        val attachments = guids
            .filter { it.isNotBlank() }
            .mapIndexed { index, guid -> AttachmentDto(guid, (index + 1).toLong()) }
        val payload = body.copy(attachments = attachments)
        val result = submitCall {
            if (isMeter) api.submitMeterPlace(payload) else api.submitLocationPlace(payload)
        }
        if (result is ApiResult.Success && isMeter) {
            val lat = payload.lat
            val lng = payload.lng
            if (!meterCode.isNullOrBlank() && lat > 0 && lng > 0) {
                meterRepository.updateCoordinates(meterCode, lat, lng)
                replaceMeterRepository.updateCoordinates(meterCode, lat, lng)
            }
        }
        return result
    }

    /** 老项目只认 HTTP 200，postLocationPlace 常返回空 body，不能按 JSON 反序列化成败来判断。 */
    private suspend fun submitCall(block: suspend () -> Response<ResponseBody>): ApiResult<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                response.body()?.close()
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

    private suspend fun <T> nullableCall(block: suspend () -> Response<T>): ApiResult<T?> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                ApiResult.Success(response.body())
            } else {
                when (val mapped = ApiExceptionHandler.handleRetrofitResponse(response)) {
                    is ApiResult.Error -> mapped
                    is ApiResult.Success -> ApiResult.Success(mapped.data)
                }
            }
        } catch (e: Exception) {
            ApiExceptionHandler.handleThrowable(e)
        }
    }
}
