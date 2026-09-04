package com.cq.iwa.feature.readmeter.data

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.readmeter.network.MeterApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BcpArchiveRepository @Inject constructor(
    private val api: MeterApi,
    private val meterRepository: MeterRepository,
) {
    suspend fun changePhone(clientCode: String, tableId: Long, phone: String): ApiResult<Unit> {
        val result = api.changeContact(
            clientCode,
            mapOf("CellPhone" to phone, "Reason" to "移动端更改联系电话"),
        ).let { response ->
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error(
                response.code(),
                response.errorBody()?.string()?.trim().orEmpty().ifBlank { "修改失败" },
            )
        }
        if (result is ApiResult.Success) {
            val meter = meterRepository.queryMeter(tableId) ?: return result
            val ext = meter.extInfo.toMutableMap()
            ext["联系电话"] = phone
            meterRepository.updateMeter(meter.copy(cellPhone = phone, extInfo = ext))
        }
        return result
    }

    suspend fun changeDescribe(meterId: Int, tableId: Long, describe: String): ApiResult<Unit> {
        val result = api.changeDescribe(meterId, describe).let { response ->
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error(
                response.code(),
                response.errorBody()?.string()?.trim().orEmpty().ifBlank { "修改失败" },
            )
        }
        if (result is ApiResult.Success) {
            val meter = meterRepository.queryMeter(tableId) ?: return result
            val ext = meter.extInfo.toMutableMap()
            ext["抄表员备注"] = describe
            meterRepository.updateMeter(meter.copy(extInfo = ext))
        }
        return result
    }
}
