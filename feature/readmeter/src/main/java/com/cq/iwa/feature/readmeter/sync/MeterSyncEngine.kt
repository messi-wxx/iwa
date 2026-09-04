package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.MeterState
import com.cq.iwa.feature.readmeter.data.MeterRepository
import com.cq.iwa.feature.readmeter.network.BookDto
import com.cq.iwa.feature.readmeter.network.LocationAttachmentDto
import com.cq.iwa.feature.readmeter.network.MeterApi
import com.cq.iwa.feature.readmeter.network.MeterDto
import com.cq.iwa.feature.readmeter.network.MeterLocationDto
import com.cq.iwa.feature.readmeter.network.ReadModelDto
import com.cq.iwa.feature.readmeter.network.parseDictionaryItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterSyncEngine @Inject constructor(
    private val repository: MeterRepository,
    private val gatewayFactory: GatewayFactory,
    private val photoUploader: PhotoUploader,
    private val mergePolicy: MeterMergePolicy,
    private val authRepository: AuthRepository,
    private val api: MeterApi,
) {
    private val bookSemaphore = Semaphore(2)
    private val errorMutex = Mutex()
    private val progressMutex = Mutex()

    suspend fun sync(
        request: SyncRequest,
        onProgress: suspend (SyncProgress) -> Unit,
    ): SyncProgress {
        val errors = mutableListOf<String>()
        var taskName = ""
        var totalProgress = ""
        Timber.tag(TAG).w(
            "sync start tasks=%s includeNet=%s",
            request.taskIds,
            request.includeNetMeter,
        )

        suspend fun emit(
            tip: String,
            percent: String = "",
            current: Int = 0,
            total: Int = 0,
            running: Boolean = true,
            finished: Boolean = false,
            catalogOnly: Boolean = false,
            title: String = "同步数据中",
        ): SyncProgress {
            val snapshot = SyncProgress(
                title = title,
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
            val user = repository.currentUser()
            val platform = repository.platform()
            val gateway = gatewayFactory.of(platform)
            val meterType = if (request.includeNetMeter) 3 else 1

            emit("刷新配置")
            when (val config = authRepository.loadAppConfig()) {
                is ApiResult.Success -> authRepository.saveUserConfigs(config.data)
                is ApiResult.Error -> {
                    errors += "配置刷新失败：${config.message}"
                    Timber.tag(TAG).w("配置刷新失败：%s", config.message)
                }
            }

            emit("正在下载表册列表")
            val serverBooks = when (val result = gateway.fetchBooks(user.name)) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    errors += "表册列表失败：${result.message}"
                    Timber.tag(TAG).w("表册列表失败：%s", result.message)
                    emptyList()
                }
            }
            persistBookList(serverBooks, user.customerCode, user.code)
            val bookNames = serverBooks.associate { (it.id.orEmpty()) to (it.taskName.orEmpty()) }

            val taskIds = request.taskIds.distinct().filter { it.isNotBlank() }
            if (taskIds.isEmpty()) {
                return emit(
                    tip = if (serverBooks.isEmpty()) "服务端没有待抄表册" else "已获取表册名单，请勾选后同步",
                    running = false,
                    finished = true,
                    catalogOnly = true,
                )
            }

            emit("正在查询本地已抄水表", current = 0, total = taskIds.size)

            coroutineScope {
                taskIds.mapIndexed { index, taskId ->
                    async {
                        try {
                            bookSemaphore.withPermit {
                                taskName = bookNames[taskId]
                                    ?: repository.queryBook(taskId)?.taskName
                                    ?: taskId
                                totalProgress = "${index + 1}/${taskIds.size}"
                                emit(
                                    tip = "正在同步表册",
                                    current = index,
                                    total = taskIds.size,
                                )
                                val bookErrors = syncOneBook(
                                    taskId = taskId,
                                    platform = platform,
                                    gateway = gateway,
                                    meterType = meterType,
                                    readName = user.name,
                                    customerCode = user.customerCode,
                                    userCode = user.code,
                                    onStage = { tip, percent ->
                                        emit(
                                            tip = tip,
                                            percent = percent,
                                            current = index,
                                            total = taskIds.size,
                                        )
                                    },
                                )
                                if (bookErrors.isNotEmpty()) {
                                    Timber.tag(TAG).w(
                                        "book %s errors=%s",
                                        taskId,
                                        bookErrors.joinToString("; "),
                                    )
                                }
                                errorMutex.withLock { errors += bookErrors }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMutex.withLock {
                                errors += "表册 $taskId 同步失败：${ApiExceptionHandler.userMessage(e)}"
                            }
                            Timber.tag(TAG).w(e, "book %s aborted", taskId)
                        }
                    }
                }.awaitAll()
            }

            emit("刷新表册", current = taskIds.size, total = taskIds.size)
            persistBookList(serverBooks, user.customerCode, user.code)
            Timber.tag(TAG).w("sync finish errors=%d", errors.size)

            return emit(
                tip = if (errors.isEmpty()) "同步完成" else "同步完成，部分失败",
                current = taskIds.size,
                total = taskIds.size,
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

    suspend fun refreshCatalog(
        onProgress: suspend (SyncProgress) -> Unit,
    ): SyncProgress {
        val errors = mutableListOf<String>()
        Timber.tag(TAG).w("refreshCatalog start")

        suspend fun emit(
            tip: String,
            running: Boolean = true,
            finished: Boolean = false,
        ): SyncProgress {
            val snapshot = SyncProgress(
                title = "获取表册名单",
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
            val user = repository.currentUser()
            val platform = repository.platform()
            val gateway = gatewayFactory.of(platform)

            emit("刷新配置")
            when (val config = authRepository.loadAppConfig()) {
                is ApiResult.Success -> authRepository.saveUserConfigs(config.data)
                is ApiResult.Error -> {
                    errors += "配置刷新失败：${config.message}"
                    Timber.tag(TAG).w("配置刷新失败：%s", config.message)
                }
            }

            emit("正在下载表册名单")
            val serverBooks = when (val result = gateway.fetchBooks(user.name)) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    errors += "表册列表失败：${result.message}"
                    Timber.tag(TAG).w("表册列表失败：%s", result.message)
                    return emit(tip = "获取表册名单失败", running = false, finished = true)
                }
            }
            persistBookList(serverBooks, user.customerCode, user.code)
            val tip = if (serverBooks.isEmpty()) {
                "服务端没有待抄表册"
            } else {
                "已获取表册名单，请勾选后同步"
            }
            Timber.tag(TAG).w("refreshCatalog finish books=%d errors=%d", serverBooks.size, errors.size)
            return emit(tip = tip, running = false, finished = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors += ApiExceptionHandler.userMessage(e)
            Timber.tag(TAG).w(e, "refreshCatalog aborted")
            return emit(tip = "获取表册名单失败", running = false, finished = true)
        }
    }

    private suspend fun syncOneBook(
        taskId: String,
        platform: MeterPlatform,
        gateway: MeterPlatformGateway,
        meterType: Int,
        readName: String,
        customerCode: String,
        userCode: String,
        onStage: suspend (tip: String, percent: String) -> Unit,
    ): List<String> {
        val errors = mutableListOf<String>()
        val pending = repository.queryPending(taskId)
        var uploadedAny = false
        if (pending.isNotEmpty()) {
            val (uploadErrors, success) = uploadPending(
                taskId = taskId,
                pending = pending,
                gateway = gateway,
                platform = platform,
                onStage = onStage,
            )
            errors += uploadErrors
            uploadedAny = success
        }
        val (locationErrors, locationUploaded) = uploadLocations(taskId, onStage)
        errors += locationErrors
        if (locationUploaded) uploadedAny = true

        onStage("校验本地表册", "")
        val localBook = repository.queryBook(taskId)
        val remoteToken = if (platform == MeterPlatform.BCP) {
            when (val result = gateway.fetchFingerprint(taskId, meterType)) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    errors += "指纹失败：$taskId ${result.message}"
                    null
                }
            }
        } else {
            localBook?.lastUpdateTime
        }
        val fingerprintMatch = !remoteToken.isNullOrBlank() && remoteToken == localBook?.fingerprint
        val skipDownload = if (platform == MeterPlatform.BCP) {
            fingerprintMatch
        } else {
            !uploadedAny && pending.isEmpty() && fingerprintMatch
        }
        if (skipDownload) {
            onStage("表册无变化，跳过下载", "100%")
            return errors
        }

        onStage("下载表册", "")
        when (val result = gateway.fetchMeters(taskId, readName, meterType)) {
            is ApiResult.Success -> {
                onStage("更新本地数据", "")
                mergeMeters(
                    taskId = taskId,
                    customerCode = customerCode,
                    userCode = userCode,
                    readName = readName,
                    dtos = result.data,
                )
                if (platform == MeterPlatform.ITWATER) {
                    errors += downloadItWaterExtInfo(taskId, gateway, onStage)
                }
            }
            is ApiResult.Error -> errors += "明细失败：$taskId ${result.message}"
        }
        val after = repository.queryBook(taskId)
        if (after != null) {
            repository.saveFingerprint(
                book = after,
                fingerprint = remoteToken ?: after.lastUpdateTime,
                lastUpdateTime = after.lastUpdateTime,
            )
        }
        return errors
    }

    private suspend fun uploadPending(
        taskId: String,
        pending: List<ReadMeterEntity>,
        gateway: MeterPlatformGateway,
        platform: MeterPlatform,
        onStage: suspend (tip: String, percent: String) -> Unit,
    ): Pair<List<String>, Boolean> {
        val errors = mutableListOf<String>()
        var anySuccess = false
        onStage("查询本地抄表并照片上传", "0%")
        val models = mutableListOf<ReadModelDto>()
        val prepared = mutableListOf<ReadMeterEntity>()
        pending.forEachIndexed { index, meter ->
            val photos = photoUploader.uploadAll(meter.photos)
            photos.fold(
                onSuccess = { guids ->
                    models += ReadModelDto(
                        id = meter.meterId,
                        meterCode = meter.meterCode,
                        pictureList = guids.filter { it.isNotBlank() },
                        reading = readingPayload(platform, meter.reading),
                        remark = meter.remark,
                        userCode = meter.clientCode,
                        readTime = meter.readTime,
                        taskId = taskId,
                    )
                    prepared += meter.copy(photos = guids.filter { it.isNotBlank() })
                },
                onFailure = { errors += "${meter.meterCode.orEmpty()} 照片失败：${it.message}" },
            )
            onStage(
                "查询本地抄表并照片上传",
                "${(index + 1) * 100 / pending.size}%",
            )
        }
        if (models.isEmpty()) return errors to anySuccess

        onStage("提交抄表数据", "0%")
        val batches = models.chunked(100)
        var successBatch = 0
        batches.forEachIndexed { batchIndex, batch ->
            val batchPrepared = prepared.drop(batchIndex * 100).take(batch.size)
            batchPrepared.forEach { repository.updateMeter(it) }
            when (val result = gateway.uploadReadings(taskId, batch)) {
                is ApiResult.Success -> {
                    val failedCodes = result.data.map { it.value }.toSet()
                    val successIds = batchPrepared
                        .filter { it.meterCode !in failedCodes && it.meterId.toString() !in failedCodes }
                        .map { it.tableId }
                    if (successIds.isNotEmpty()) {
                        repository.markUploaded(successIds)
                        anySuccess = true
                    }
                    result.data.forEach { msg ->
                        errors += "水表${msg.value}提交失败,原因:${msg.text}".trim()
                    }
                    successBatch++
                    onStage("提交抄表数据", "${successBatch * 100 / batches.size}%")
                }
                is ApiResult.Error -> errors += "读数上传失败：$taskId ${result.message}"
            }
        }
        return errors to anySuccess
    }

    private suspend fun uploadLocations(
        taskId: String,
        onStage: suspend (tip: String, percent: String) -> Unit,
    ): Pair<List<String>, Boolean> {
        val pending = repository.queryLocationPending(taskId)
        if (pending.isEmpty()) return emptyList<String>() to false
        val errors = mutableListOf<String>()
        val payloads = mutableListOf<Pair<Long, MeterLocationDto>>()
        pending.forEachIndexed { index, meter ->
            onStage("正在上传水表环境图片", "${(index + 1) * 100 / pending.size}%")
            val env = meter.usableEnvPhotos()
            val guids = photoUploader.uploadAll(env).getOrElse { error ->
                errors += "环境照片失败：${meter.meterCode.orEmpty()} ${error.message}"
                emptyList()
            }
            if (guids.size != env.size) return@forEachIndexed
            payloads += meter.tableId to MeterLocationDto(
                linkId = meter.meterId.toLong(),
                lat = meter.latitude,
                lng = meter.longitude,
                attachments = guids.mapIndexed { i, guid ->
                    LocationAttachmentDto(attachmentId = guid, seq = i + 1)
                },
            )
        }
        if (payloads.isEmpty()) return errors to false
        var anySuccess = false
        val batches = payloads.chunked(100)
        batches.forEachIndexed { batchIndex, chunk ->
            onStage("正在上传水表位置数据", "${(batchIndex + 1) * 100 / batches.size}%")
            when (val result = ApiExceptionHandler.safeApiCall { api.uploadLocations(chunk.map { it.second }) }) {
                is ApiResult.Success -> {
                    repository.clearEnvPhotos(chunk.map { it.first })
                    anySuccess = true
                }
                is ApiResult.Error -> errors += "上传位置失败：$taskId ${result.message}"
            }
        }
        return errors to anySuccess
    }

    private suspend fun persistBookList(
        serverBooks: List<BookDto>,
        customerCode: String,
        userCode: String,
    ) {
        val entities = serverBooks.mapNotNull { dto ->
            val taskId = dto.id ?: return@mapNotNull null
            MeterBookEntity(
                taskId = taskId,
                taskName = dto.taskName,
                customerCode = customerCode,
                userCode = userCode,
                taskType = 1,
                taskState = 0,
                createTime = dto.createTime,
                lastUpdateTime = dto.lastUpdateTime,
            )
        }
        repository.upsertBooks(entities)
        repository.markMissingBooksCompleted(entities.map { it.taskId }.toSet())
    }

    private suspend fun mergeMeters(
        taskId: String,
        customerCode: String,
        userCode: String,
        readName: String,
        dtos: List<MeterDto>,
    ) {
        val localAll = repository.queryLocalAll(taskId)
        val localById = localAll.associateBy { it.meterId }
        val serverIds = dtos.map { it.id }.toSet()
        dtos.forEach { dto ->
            val local = localById[dto.id]
            val merged = mergePolicy.toEntity(dto, taskId, customerCode, userCode, readName, local)
            if (local == null) {
                repository.insertMeter(merged)
            } else {
                repository.updateMeter(merged)
            }
        }
        localAll.filter { it.meterId !in serverIds }.forEach { leftover ->
            repository.updateMeter(leftover.copy(state = MeterState.DELETED))
        }
    }

    private suspend fun downloadItWaterExtInfo(
        taskId: String,
        gateway: MeterPlatformGateway,
        onStage: suspend (tip: String, percent: String) -> Unit,
    ): List<String> {
        if (!repository.configYes("LoadExtInfo")) return emptyList()
        val localAll = repository.queryLocalAll(taskId)
            .filter { it.state != MeterState.DELETED }
        val codes = localAll.mapNotNull { it.meterCode?.takeIf { code -> code.isNotBlank() } }
            .distinct()
        if (codes.isEmpty()) return emptyList()
        val errors = mutableListOf<String>()
        val byCode = localAll.associateBy { it.meterCode }
        val batches = codes.chunked(EXT_BATCH)
        onStage("正在下载表册的扩展数据", "0%")
        batches.forEachIndexed { index, batch ->
            when (val result = gateway.fetchExtInfo(batch)) {
                is ApiResult.Success -> {
                    result.data.forEach { item ->
                        val code = item.text?.takeIf { it.isNotBlank() } ?: return@forEach
                        val meter = byCode[code] ?: return@forEach
                        repository.updateMeter(meter.copy(extInfo = parseDictionaryItem(item.value)))
                    }
                }
                is ApiResult.Error -> errors += "扩展数据失败：$taskId ${result.message}"
            }
            onStage(
                "正在下载表册的扩展数据",
                "${(index + 1) * 100 / batches.size}%",
            )
        }
        return errors
    }

    private fun readingPayload(
        platform: MeterPlatform,
        reading: String?,
    ) = if (platform == MeterPlatform.BCP) {
        reading?.let { JsonPrimitive(it) }
    } else {
        reading?.toDoubleOrNull()?.let { JsonPrimitive(it) }
    }

    private companion object {
        const val TAG = "MeterSync"
        const val EXT_BATCH = 100
    }
}
