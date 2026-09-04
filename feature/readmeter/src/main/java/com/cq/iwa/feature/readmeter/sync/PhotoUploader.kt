package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.data.MeterLocalStore
import com.cq.iwa.feature.readmeter.network.FileApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoUploader @Inject constructor(
    private val fileApi: FileApi,
    private val localStore: MeterLocalStore,
    private val authRepository: AuthRepository,
) {
    private val semaphore = Semaphore(4)

    suspend fun uploadAll(
        paths: List<String>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<List<String>> = coroutineScope {
        if (paths.isEmpty()) return@coroutineScope Result.success(emptyList())
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        val total = paths.size
        runCatching {
            paths.map { path ->
                async {
                    semaphore.withPermit {
                        val guid = uploadOne(path)
                        onProgress(done.incrementAndGet(), total)
                        guid
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun uploadOne(path: String): String {
        if (path.isBlank() || path == "button") return ""
        val file = resolveLocalFile(path)
        if (file == null) return path
        val body = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType()),
        )
        return when (val result = ApiExceptionHandler.safeApiCall { fileApi.uploadFile(body) }) {
            is ApiResult.Success -> result.data.guid.ifBlank {
                throw java.io.IOException("照片上传未返回文件编号")
            }
            is ApiResult.Error -> throw java.io.IOException(result.message)
        }
    }

    private suspend fun resolveLocalFile(path: String): File? {
        val user = authRepository.getCurrentUser()
        return if (user != null) {
            localStore.resolveLocalFile(path, user.customerCode, user.code)
        } else {
            val direct = File(path)
            direct.takeIf { it.exists() && it.isFile }
        }
    }
}
