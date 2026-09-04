package com.cq.iwa.feature.replacemeter.sync

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.sync.PhotoUploader
import com.cq.iwa.feature.readmeter.sync.SyncProgress
import com.cq.iwa.feature.replacemeter.REPLACE_TASK_TYPE
import com.cq.iwa.feature.replacemeter.REPLACE_UPLOAD_BATCH
import com.cq.iwa.feature.replacemeter.ReplaceMeterState
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import com.cq.iwa.feature.replacemeter.data.computeProgress
import com.cq.iwa.feature.replacemeter.network.AttachmentDto
import com.cq.iwa.feature.replacemeter.network.ReplaceBookDto
import com.cq.iwa.feature.replacemeter.network.ReplaceLocationDto
import com.cq.iwa.feature.replacemeter.network.ReplaceMeterApi
import com.cq.iwa.feature.replacemeter.network.ReplaceMeterDto
import com.cq.iwa.feature.replacemeter.network.ReplaceModelDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplaceMeterSyncEngine @Inject constructor(
    private val repository: ReplaceMeterRepository,
    private val api: ReplaceMeterApi,
    private val photoUploader: PhotoUploader,
    private val authRepository: AuthRepository,
) {
    private val progressMutex = Mutex()

    suspend fun refreshCatalog(
        onProgress: suspend (SyncProgress) -> Unit,
    ): SyncProgress {
        val errors = mutableListOf<String>()
        Timber.tag(TAG).w("refreshCatalog start")
        suspend fun emit(tip: String, running: Boolean = true, finished: Boolean = false): SyncProgress {
            val snapshot = SyncProgress(
                title = "获取换表任务",
                tip = tip,
                running = running,
                finished = finished,
                catalogOnly = true,
                errors = errors.toList(),
            )
            progressMutex.withLock { onProgress(snapshot) }
            return snapshot
        }

        try {
            val platform = repository.platform()
            if (platform != MeterPlatform.EDC) {
                return emit("换表任务仅支持 EDC 平台", running = false, finished = true)
            }
            emit("刷新配置")
            when (val config = authRepository.loadAppConfig()) {
                is ApiResult.Success -> authRepository.saveUserConfigs(config.data)
                is ApiResult.Error -> {
                    errors += "配置刷新失败：${config.message}"
                    Timber.tag(TAG).w("配置刷新失败：%s", config.message)
                }
            }
            emit("正在下载换表任务名单")
            val user = repository.currentUser()
            val books = when (val result = ApiExceptionHandler.safeApiCall { api.getTasks(user.name) }) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    errors += "换表任务列表失败：${result.message}"
                    Timber.tag(TAG).w("换表任务列表失败：%s", result.message)
                    return emit("获取换表任务失败", running = false, finished = true)
                }
            }
            persistBooks(books, user.customerCode, user.code)
            val tip = if (books.isEmpty()) "服务端没有待换表任务" else "已获取任务名单，请勾选后同步"
            Timber.tag(TAG).w("refreshCatalog finish books=%d errors=%d", books.size, errors.size)
            return emit(tip, running = false, finished = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += ApiExceptionHandler.userMessage(e)
            Timber.tag(TAG).w(e, "refreshCatalog aborted")
            return emit("获取换表任务失败", running = false, finished = true)
        }
    }

    suspend fun sync(
        taskIds: List<String>,
        onProgress: suspend (SyncProgress) -> Unit,
    ): SyncProgress {
        val errors = mutableListOf<String>()
        var taskName = ""
        var totalProgress = ""
        Timber.tag(TAG).w("sync start tasks=%s", taskIds)

        suspend fun emit(
            tip: String,
            percent: String = "",
            current: Int = 0,
            total: Int = 0,
            running: Boolean = true,
            finished: Boolean = false,
            catalogOnly: Boolean = false,
        ): SyncProgress {
            val snapshot = SyncProgress(
                title = "同步换表数据",
                taskName = taskName,
                totalProgress = totalProgress,
                tip = tip,
                percent = percent,
                current = current,
                total = total,
                running = running,
                finished = finished,
                catalogOnly = catalogOnly,
                errors = errors.toList(),
            )
            progressMutex.withLock { onProgress(snapshot) }
            return snapshot
        }

        try {
            val platform = repository.platform()
            if (platform != MeterPlatform.EDC) {
                return emit("换表任务仅支持 EDC 平台", running = false, finished = true)
            }
            val user = repository.currentUser()
            emit("刷新配置")
            when (val config = authRepository.loadAppConfig()) {
                is ApiResult.Success -> authRepository.saveUserConfigs(config.data)
                is ApiResult.Error -> {
                    errors += "配置刷新失败：${config.message}"
                    Timber.tag(TAG).w("配置刷新失败：%s", config.message)
                }
            }

            emit("正在下载换表任务名单")
            val serverBooks = when (val result = ApiExceptionHandler.safeApiCall { api.getTasks(user.name) }) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    errors += "换表任务列表失败：${result.message}"
                    Timber.tag(TAG).w("换表任务列表失败：%s", result.message)
                    emptyList()
                }
            }
            persistBooks(serverBooks, user.customerCode, user.code)
            val names = serverBooks.associate { it.id.orEmpty() to it.taskName }
            val selected = taskIds.distinct().filter { it.isNotBlank() }
            if (selected.isEmpty()) {
                return emit(
                    tip = if (serverBooks.isEmpty()) "服务端没有待换表任务" else "已获取任务名单，请勾选后同步",
                    running = false,
                    finished = true,
                    catalogOnly = true,
                )
            }

            selected.forEachIndexed { index, taskId ->
                try {
                    taskName = names[taskId] ?: repository.queryBook(taskId)?.taskName ?: taskId
                    totalProgress = "${index + 1}/${selected.size}"
                    emit("正在同步换表任务", current = index, total = selected.size)
                    val oneErrors = syncOne(
                        taskId = taskId,
                        lastUpdateTime = serverBooks.firstOrNull { it.id == taskId }?.lastUpdateTime,
                        readName = user.name,
                        customerCode = user.customerCode,
                        userCode = user.code,
                        onStage = { tip, percent ->
                            emit(tip, percent = percent, current = index, total = selected.size)
                        },
                    )
                    if (oneErrors.isNotEmpty()) {
                        Timber.tag(TAG).w("task %s errors=%s", taskId, oneErrors.joinToString("; "))
                    }
                    errors += oneErrors
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors += "任务 $taskId 同步失败：${ApiExceptionHandler.userMessage(e)}"
                    Timber.tag(TAG).w(e, "task %s aborted", taskId)
                }
            }
            persistBooks(serverBooks, user.customerCode, user.code)
            Timber.tag(TAG).w("sync finish errors=%d", errors.size)
            return emit(
                tip = if (errors.isEmpty()) "同步完成" else "同步完成，部分失败",
                current = selected.size,
                total = selected.size,
                running = false,
                finished = true,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += ApiExceptionHandler.userMessage(e)
            Timber.tag(TAG).w(e, "sync aborted")
            return emit(
                tip = "同步失败",
                running = false,
                finished = true,
            )
        }
    }

    private suspend fun syncOne(
        taskId: String,
        lastUpdateTime: String?,
        readName: String,
        customerCode: String,
        userCode: String,
        onStage: suspend (String, String) -> Unit,
    ): List<String> {
        val errors = mutableListOf<String>()
        val pending = repository.queryPending(taskId)
        var uploaded = false
        if (pending.isNotEmpty()) {
            onStage("正在上传换表照片", "")
            val (uploadErrors, successIds) = uploadPending(pending, onStage)
            errors += uploadErrors
            uploaded = successIds.isNotEmpty()
            if (successIds.isNotEmpty()) repository.markUploaded(successIds)
        }

        val localBook = repository.queryBook(taskId)
        if (!uploaded && pending.isEmpty() &&
            !lastUpdateTime.isNullOrBlank() &&
            lastUpdateTime == localBook?.fingerprint
        ) {
            onStage("任务无变化，跳过下载", "100%")
            return errors
        }

        onStage("正在下载换表任务", "")
        when (val result = ApiExceptionHandler.safeApiCall { api.getMeters(taskId, readName) }) {
            is ApiResult.Success -> {
                val entities = result.data.map { it.toEntity(customerCode, userCode, taskId, readName) }
                repository.mergeMeters(taskId, entities, lastUpdateTime)
                onStage("下载完成", "100%")
            }
            is ApiResult.Error -> errors += "下载失败：$taskId ${result.message}"
        }
        return errors
    }

    private suspend fun uploadPending(
        pending: List<ReplaceMeterEntity>,
        onStage: suspend (String, String) -> Unit,
    ): Pair<List<String>, List<Long>> {
        val errors = mutableListOf<String>()
        val success = mutableListOf<Long>()
        val models = mutableListOf<Pair<Long, ReplaceModelDto>>()
        val locations = mutableListOf<Pair<Long, ReplaceLocationDto>>()
        pending.forEachIndexed { index, meter ->
            onStage("正在上传换表照片", percent(index, pending.size))
            val oldGuids = photoUploader.uploadAll(meter.oldPhotos.filter { it.isNotBlank() && it != "button" })
                .getOrElse { error ->
                    errors += "旧表照片失败：${meter.oldMeterCode} ${error.message}"
                    return@forEachIndexed
                }.filter { it.isNotBlank() }
            val newGuids = photoUploader.uploadAll(meter.newPhotos.filter { it.isNotBlank() && it != "button" })
                .getOrElse { error ->
                    errors += "新表照片失败：${meter.oldMeterCode} ${error.message}"
                    return@forEachIndexed
                }.filter { it.isNotBlank() }
            if (oldGuids.isEmpty() && newGuids.isEmpty()) return@forEachIndexed
            models += meter.tableId to ReplaceModelDto(
                id = meter.meterId,
                taskId = meter.taskId,
                oldReading = meter.oldReading?.toDoubleOrNull(),
                oldPicList = oldGuids,
                newMeterCode = meter.newMeterCode,
                newReading = meter.newReading?.toDoubleOrNull(),
                newCaliber = meter.caliber,
                newPicList = newGuids,
                installType = meter.installType,
                verifyOrg = meter.verifyOrg,
                verifyDate = meter.verifyDate,
                verifyExpireDate = meter.verifyExpireDate,
            )
            if (meter.locationPending && meter.envPhotos.isNotEmpty()) {
                val envGuids = photoUploader.uploadAll(meter.envPhotos.filter { it.isNotBlank() && it != "button" })
                    .getOrElse { error ->
                        errors += "环境照片失败：${meter.oldMeterCode} ${error.message}"
                        emptyList()
                    }
                if (envGuids.size == meter.envPhotos.filter { it.isNotBlank() && it != "button" }.size) {
                    locations += meter.tableId to ReplaceLocationDto(
                        linkId = meter.meterId.toLong(),
                        lat = meter.latitude,
                        lng = meter.longitude,
                        attachments = envGuids.mapIndexed { i, guid ->
                            AttachmentDto(attachmentId = guid, seq = i + 1)
                        },
                    )
                }
            }
        }
        models.chunked(REPLACE_UPLOAD_BATCH).forEach { chunk ->
            onStage("正在上传换表数据", "")
            when (val result = ApiExceptionHandler.safeApiCall { api.upload(chunk.map { it.second }) }) {
                is ApiResult.Success -> success += chunk.map { it.first }
                is ApiResult.Error -> errors += "上传换表失败：${result.message}"
            }
        }
        val uploadedSet = success.toSet()
        locations.filter { it.first in uploadedSet }.chunked(REPLACE_UPLOAD_BATCH).forEach { chunk ->
            onStage("正在上传换表位置", "")
            when (val result = ApiExceptionHandler.safeApiCall { api.uploadLocations(chunk.map { it.second }) }) {
                is ApiResult.Success -> repository.clearLocationPending(chunk.map { it.first })
                is ApiResult.Error -> errors += "上传位置失败：${result.message}"
            }
        }
        return errors to success
    }

    private suspend fun persistBooks(
        books: List<ReplaceBookDto>,
        customerCode: String,
        userCode: String,
    ) {
        repository.upsertBooks(
            books.mapNotNull { dto ->
                val id = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                MeterBookEntity(
                    taskId = id,
                    taskName = dto.taskName,
                    customerCode = customerCode,
                    userCode = userCode,
                    taskType = REPLACE_TASK_TYPE,
                    taskState = 0,
                    createTime = dto.createTime,
                    lastUpdateTime = dto.lastUpdateTime,
                )
            },
        )
    }

    private fun percent(index: Int, total: Int): String {
        if (total <= 0) return ""
        return "${((index + 1) * 100 / total).coerceAtMost(100)}%"
    }

    private companion object {
        const val TAG = "ReplaceMeterSync"
    }
}

private fun ReplaceMeterDto.toEntity(
    customerCode: String,
    userCode: String,
    taskId: String,
    replaceName: String,
): ReplaceMeterEntity {
    val progress = computeProgress(oldReading, newMeterCode)
    return ReplaceMeterEntity(
        meterId = id,
        taskId = taskId.ifBlank { this.taskId.orEmpty() },
        customerCode = customerCode,
        userCode = userCode,
        clientCode = clientCode,
        address = address,
        caliber = caliber,
        oldMeterCode = oldMeterCode,
        oldReading = oldReading,
        newMeterCode = newMeterCode,
        newReading = newReading,
        replaceName = this.replaceName ?: replaceName,
        sort = sort,
        isReplace = isReplace,
        state = ReplaceMeterState.SYNCED,
        progress = progress,
        latitude = latitude,
        longitude = longitude,
        replaceRyFlux = replaceRYFlux,
        extInfo = extInfo,
        installType = installType,
        verifyOrg = verifyOrg,
        verifyDate = verifyDate,
        verifyExpireDate = verifyExpireDate,
    )
}
