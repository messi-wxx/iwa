package com.cq.iwa.feature.installation.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.installation.network.InstAddMeterBody
import com.cq.iwa.feature.installation.network.InstApi
import com.cq.iwa.feature.installation.network.InstDocumentDownBody
import com.cq.iwa.feature.installation.network.InstExtendBody
import com.cq.iwa.feature.installation.network.InstListRequest
import com.cq.iwa.feature.installation.network.InstLogRequest
import com.cq.iwa.feature.installation.network.InstProjectIdBody
import com.cq.iwa.feature.installation.network.InstRecordInfoBody
import com.cq.iwa.feature.installation.network.InstRejectBody
import com.cq.iwa.feature.installation.network.InstUrgeBody
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstRepository @Inject constructor(
    private val api: InstApi,
    @ApplicationContext private val context: Context,
) {

    suspend fun getWorkbenchList(pageSize: Int, currentPage: Int, body: InstListRequest) =
        ApiExceptionHandler.safeApiCall { api.getWorkbenchList(pageSize, currentPage, body) }

    suspend fun getAllProjectList(pageSize: Int, currentPage: Int, body: InstListRequest) =
        ApiExceptionHandler.safeApiCall { api.getAllProjectList(pageSize, currentPage, body) }

    suspend fun getWorkbenchCount() =
        ApiExceptionHandler.safeApiCall { api.getWorkbenchCount() }

    suspend fun getProjectSketch(id: Int) =
        ApiExceptionHandler.safeApiCall { api.getProjectSketchById(id) }

    suspend fun getTaskDetail(taskId: String) =
        ApiExceptionHandler.safeApiCall { api.getTaskDetail(taskId) }

    suspend fun getProcessInstanceDetail(projectId: Int) =
        ApiExceptionHandler.safeApiCall { api.getProcessInstanceDetail(projectId) }

    suspend fun getRejectTargets(taskId: String) =
        ApiExceptionHandler.safeApiCall { api.getRejectTargets(taskId) }

    suspend fun rejectTask(body: InstRejectBody) = mutate { api.rejectTask(body) }

    suspend fun extendTask(body: InstExtendBody) = mutate { api.extendTask(body) }

    suspend fun completeTask(json: String): ApiResult<Unit> {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        return mutate { api.completeTask(body) }
    }

    suspend fun followProject(projectId: Int) = mutate { api.followProject(projectId) }

    suspend fun urgeProject(projectId: Int, content: String) =
        mutate { api.urgeProject(InstUrgeBody(projectId, content)) }

    suspend fun getProcessOverview(projectId: Int) =
        ApiExceptionHandler.safeApiCall { api.getProcessOverview(projectId) }

    suspend fun claimTask(taskId: String) = mutate { api.claimTask(taskId) }

    suspend fun getDictionaryOption(code: String): ApiResult<String> =
        rawBody { api.getDictionaryOptionList(code) }

    suspend fun getOptionList(code: String): ApiResult<String> =
        rawBody { api.getOptionList(code) }

    suspend fun getMeterInstallInfoList(projectId: Int) =
        ApiExceptionHandler.safeApiCall { api.getMeterInstallInfoList(body = InstProjectIdBody(projectId)) }

    suspend fun getMeterRecordInfoList(body: InstRecordInfoBody) =
        ApiExceptionHandler.safeApiCall { api.getMeterRecordInfoList(body = body) }

    suspend fun postMeter(body: InstAddMeterBody) = mutate { api.postMeterRecordInstall(body) }

    suspend fun updateMeter(body: InstAddMeterBody) = mutate { api.updateMeterRecordInfo(body) }

    suspend fun getMeterById(id: Int) =
        ApiExceptionHandler.safeApiCall { api.getMeterRecordById(id) }

    suspend fun deleteMeter(id: Int) = mutate { api.deleteMeterRecordById(id) }

    suspend fun getProjectLogs(body: InstLogRequest) =
        ApiExceptionHandler.safeApiCall { api.getProjectLogList(body = body) }

    suspend fun uploadExcel(projectId: Int, file: File): ApiResult<Unit> {
        val part = MultipartBody.Part.createFormData(
            "Form",
            file.name,
            file.readBytes().toRequestBody(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull(),
            ),
        )
        return mutate { api.uploadExcel(projectId, part) }
    }

    suspend fun downloadContract(projectId: Int): ApiResult<Uri> {
        return saveStreaming { api.contractDown(projectId) }
    }

    suspend fun downloadDocument(projectId: Int, type: Int): ApiResult<Uri> {
        return saveStreaming { api.documentDown(InstDocumentDownBody(projectId, type)) }
    }

    private suspend fun saveStreaming(block: suspend () -> Response<ResponseBody>): ApiResult<Uri> {
        return try {
            val response = block()
            if (!response.isSuccessful) {
                return when (val mapped = ApiExceptionHandler.handleRetrofitResponse(response)) {
                    is ApiResult.Error -> mapped
                    is ApiResult.Success -> ApiResult.Error(message = "下载失败")
                }
            }
            val body = response.body() ?: return ApiResult.Error(message = "响应体为空")
            val name = fileName(response) ?: "download_${System.currentTimeMillis()}.file"
            val uri = saveToDownloads(name, body.byteStream())
                ?: return ApiResult.Error(message = "保存文件失败")
            ApiResult.Success(uri)
        } catch (e: Exception) {
            ApiExceptionHandler.handleThrowable(e)
        }
    }

    private fun saveToDownloads(fileName: String, input: InputStream): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType(fileName))
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { input.copyTo(it) }
                uri
            }.getOrElse {
                context.contentResolver.delete(uri, null, null)
                null
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists() && !dir.mkdirs()) return null
            val file = File(dir, fileName)
            runCatching {
                file.outputStream().use { input.copyTo(it) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType(fileName)), null)
                Uri.fromFile(file)
            }.getOrNull()
        }
    }

    private fun fileName(response: Response<ResponseBody>): String? {
        val disposition = response.headers()["Content-Disposition"] ?: return null
        Regex("""filename\*=\s*UTF-8''(.+)""", RegexOption.IGNORE_CASE).find(disposition)?.let {
            return runCatching { URLDecoder.decode(it.groupValues[1], "UTF-8") }.getOrNull()
        }
        Regex("""filename=(?:"([^"]+)"|([^;]+))""").find(disposition)?.let {
            return it.groupValues[1].ifBlank { it.groupValues[2] }.trim()
        }
        return null
    }

    private fun mimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }

    private suspend fun rawBody(block: suspend () -> Response<ResponseBody>): ApiResult<String> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.string() ?: "[]")
            } else {
                when (val mapped = ApiExceptionHandler.handleRetrofitResponse(response)) {
                    is ApiResult.Error -> mapped
                    is ApiResult.Success -> ApiResult.Success("[]")
                }
            }
        } catch (e: Exception) {
            ApiExceptionHandler.handleThrowable(e)
        }
    }

    private suspend fun mutate(block: suspend () -> Response<ResponseBody>): ApiResult<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
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
