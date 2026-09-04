package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.feature.readmeter.network.BookDto
import com.cq.iwa.feature.readmeter.network.ErrorMsgDto
import com.cq.iwa.feature.readmeter.network.MeterDto
import com.cq.iwa.feature.readmeter.network.MeterExtInfoDto
import com.cq.iwa.feature.readmeter.network.ReadModelDto

interface MeterPlatformGateway {
    suspend fun fetchBooks(readName: String): ApiResult<List<BookDto>>
    suspend fun fetchMeters(taskId: String, readName: String, meterType: Int): ApiResult<List<MeterDto>>
    suspend fun uploadReadings(
        taskId: String,
        models: List<ReadModelDto>,
    ): ApiResult<List<ErrorMsgDto>>

    suspend fun fetchFingerprint(taskId: String, meterType: Int): ApiResult<String?>

    suspend fun fetchExtInfo(meterCodes: List<String>): ApiResult<List<MeterExtInfoDto>> =
        ApiResult.Success(emptyList())
}

internal fun retrofit2.Response<*>.asUnitSuccess(): ApiResult<List<ErrorMsgDto>> {
    return if (isSuccessful) {
        ApiResult.Success(emptyList())
    } else {
        val message = errorBody()?.string()?.trim().orEmpty()
            .ifBlank { "上传失败 (${code()})" }
        ApiResult.Error(code(), message)
    }
}
