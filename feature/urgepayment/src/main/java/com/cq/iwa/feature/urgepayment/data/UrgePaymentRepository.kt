package com.cq.iwa.feature.urgepayment.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.readmeter.data.MeterRepository
import com.cq.iwa.feature.urgepayment.network.ChangePhoneBodyDto
import com.cq.iwa.feature.urgepayment.network.UrgeClientSearchDto
import com.cq.iwa.feature.urgepayment.network.UrgeFeeClientDto
import com.cq.iwa.feature.urgepayment.network.UrgeFeeDetailDto
import com.cq.iwa.feature.urgepayment.network.UrgeMeterDto
import com.cq.iwa.feature.urgepayment.network.UrgePaymentApi
import com.cq.iwa.feature.urgepayment.network.UrgeReadListDto
import com.cq.iwa.feature.urgepayment.network.UrgeSortClientDto
import com.cq.iwa.feature.urgepayment.network.UrgeTaskDto
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrgePaymentRepository @Inject constructor(
    private val api: UrgePaymentApi,
    private val meterRepository: MeterRepository,
) {

    suspend fun displayClientCode(): Boolean =
        meterRepository.configYes("ReaderListDisplayClientCode")

    suspend fun getUrgeTasks(): ApiResult<List<UrgeTaskDto>> =
        ApiExceptionHandler.safeApiCall { api.getUrgeTasks() }

    suspend fun getMetersForBook(bookId: Int): ApiResult<List<UrgeMeterDto>> =
        ApiExceptionHandler.safeApiCall { api.getMetersForBook(bookId) }

    suspend fun searchClients(keyword: String): ApiResult<UrgeClientSearchDto> =
        ApiExceptionHandler.safeApiCall { api.searchClients(keyword, 1, 100) }

    suspend fun getDetailByCode(code: String): ApiResult<UrgeFeeClientDto> =
        ApiExceptionHandler.safeApiCall { api.getDetailByCode(code) }

    suspend fun getNopayFees(clientId: Int): ApiResult<List<UrgeFeeDetailDto>> =
        ApiExceptionHandler.safeApiCall { api.getNopayFees(clientId) }

    suspend fun getReadHistory(deviceId: Int): ApiResult<UrgeReadListDto> =
        ApiExceptionHandler.safeApiCall { api.getReadHistory(deviceId, 20, 1) }

    suspend fun nextClient(clientId: Int): ApiResult<UrgeSortClientDto> =
        ApiExceptionHandler.safeApiCall { api.nextSortClient(clientId) }

    suspend fun previousClient(clientId: Int): ApiResult<UrgeSortClientDto> =
        ApiExceptionHandler.safeApiCall { api.previousSortClient(clientId) }

    suspend fun changePhone(clientCode: String, phone: String): ApiResult<Unit> =
        voidCall { api.changePhone(clientCode, ChangePhoneBodyDto(CellPhone = phone)) }

    suspend fun changeRemark(deviceId: Int, remark: String): ApiResult<Unit> =
        voidCall { api.changeRemark(deviceId, remark) }

    suspend fun openValve(deviceId: Int): ApiResult<Unit> =
        voidCall { api.openValve(deviceId.toLong()) }

    suspend fun closeValve(deviceId: Int): ApiResult<Unit> =
        voidCall { api.closeValve(deviceId.toLong()) }

    /** 改电话/备注/开关阀常返回空 body，只认 HTTP 成功。 */
    private suspend fun voidCall(block: suspend () -> Response<ResponseBody>): ApiResult<Unit> {
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
}
