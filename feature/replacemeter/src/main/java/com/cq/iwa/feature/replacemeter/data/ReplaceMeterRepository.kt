package com.cq.iwa.feature.replacemeter.data

import com.cq.iwa.core.database.dao.MeterBookDao
import com.cq.iwa.core.database.dao.ReplaceMeterDao
import com.cq.iwa.core.database.dao.UserConfigDao
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.database.entity.UserEntity
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.data.MeterLocalStore
import com.cq.iwa.feature.replacemeter.REPLACE_TASK_TYPE
import com.cq.iwa.feature.replacemeter.ReplaceMeterState
import com.cq.iwa.feature.replacemeter.ReplaceProgress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplaceMeterRepository @Inject constructor(
    private val bookDao: MeterBookDao,
    private val meterDao: ReplaceMeterDao,
    private val userConfigDao: UserConfigDao,
    private val authRepository: AuthRepository,
    private val localStore: MeterLocalStore,
) {
    private val dbMutex = Mutex()

    suspend fun currentUser(): UserEntity =
        authRepository.getCurrentUser() ?: error("未登录")

    suspend fun platform(): MeterPlatform {
        val config = userConfigDao.findByName("apiSource")
        return MeterPlatform.fromConfig(config?.configValue)
    }

    suspend fun configYes(name: String): Boolean {
        val value = userConfigDao.findByName(name)?.configValue
        return value.equals("yes", ignoreCase = true)
    }

    suspend fun configValue(name: String): String? =
        userConfigDao.findByName(name)?.configValue?.takeIf { it.isNotBlank() }

    suspend fun queryBooks(): List<MeterBookEntity> {
        val user = currentUser()
        return bookDao.queryBooks(user.customerCode, user.code, REPLACE_TASK_TYPE)
    }

    suspend fun queryBook(taskId: String): MeterBookEntity? {
        val user = currentUser()
        return bookDao.queryByTaskIdAndType(user.customerCode, user.code, taskId, REPLACE_TASK_TYPE)
    }

    suspend fun queryVisibleMeters(taskId: String): List<ReplaceMeterEntity> {
        val user = currentUser()
        return meterDao.queryByTask(user.customerCode, user.code, taskId)
    }

    suspend fun queryPagedMeters(
        taskId: String,
        groupName: String,
        finished: Boolean,
        pageSize: Int,
        offset: Int,
    ): List<ReplaceMeterEntity> {
        val user = currentUser()
        return meterDao.queryPaged(
            user.customerCode,
            user.code,
            taskId,
            groupName,
            if (finished) 1 else 0,
            pageSize,
            offset,
        )
    }

    suspend fun countsByGroup(taskId: String, groupName: String): Pair<Int, Int> {
        val user = currentUser()
        val unfinished = meterDao.countUnfinishedByGroup(user.customerCode, user.code, taskId, groupName)
        val finished = meterDao.countFinishedByGroup(user.customerCode, user.code, taskId, groupName)
        return unfinished to finished
    }

    suspend fun queryPending(taskId: String): List<ReplaceMeterEntity> {
        val user = currentUser()
        return meterDao.queryPending(user.customerCode, user.code, taskId)
    }

    suspend fun queryMeter(tableId: Long): ReplaceMeterEntity? {
        val user = currentUser()
        val meter = meterDao.queryByTableId(tableId) ?: return null
        return meter.takeIf { it.customerCode == user.customerCode && it.userCode == user.code }
    }

    suspend fun counts(taskId: String): Pair<Int, Int> {
        val user = currentUser()
        val unfinished = meterDao.countUnfinished(user.customerCode, user.code, taskId)
        val finished = meterDao.countFinished(user.customerCode, user.code, taskId)
        return unfinished to finished
    }

    suspend fun bookCounts(taskId: String): Triple<Int, Int, Int> {
        val user = currentUser()
        val total = meterDao.countVisible(user.customerCode, user.code, taskId)
        val unfinished = meterDao.countUnfinished(user.customerCode, user.code, taskId)
        val finished = meterDao.countFinished(user.customerCode, user.code, taskId)
        return Triple(total, unfinished, finished)
    }

    suspend fun search(keyword: String, taskId: String? = null): List<ReplaceMeterEntity> {
        val user = currentUser()
        val key = "%${keyword.trim()}%"
        return meterDao.search(user.customerCode, user.code, key)
            .filter { taskId == null || it.taskId == taskId }
    }

    suspend fun queryGroups(taskId: String): List<String> {
        val user = currentUser()
        return meterDao.queryGroups(user.customerCode, user.code, taskId)
    }

    suspend fun hasDuplicateNewCode(taskId: String, newMeterCode: String, excludeTableId: Long): Boolean {
        val user = currentUser()
        return meterDao.queryDuplicateNewCode(
            user.customerCode,
            user.code,
            taskId,
            newMeterCode,
            excludeTableId,
        ) != null
    }

    suspend fun queryNext(
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String = "",
    ): ReplaceMeterEntity? {
        val user = currentUser()
        return meterDao.queryNext(user.customerCode, user.code, taskId, sort, tableId, groupName)
    }

    suspend fun queryPrevious(
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String = "",
    ): ReplaceMeterEntity? {
        val user = currentUser()
        return meterDao.queryPrevious(user.customerCode, user.code, taskId, sort, tableId, groupName)
    }

    suspend fun saveMeter(meter: ReplaceMeterEntity) = dbMutex.withLock {
        meterDao.update(meter)
    }

    suspend fun markUploaded(tableIds: List<Long>) = dbMutex.withLock {
        tableIds.forEach { id ->
            val meter = meterDao.queryByTableId(id) ?: return@forEach
            meterDao.update(meter.copy(state = ReplaceMeterState.SYNCED, locationPending = false))
        }
    }

    suspend fun clearLocationPending(tableIds: List<Long>) = dbMutex.withLock {
        tableIds.forEach { id ->
            val meter = meterDao.queryByTableId(id) ?: return@forEach
            meterDao.update(meter.copy(locationPending = false, envPhotos = emptyList()))
        }
    }

    suspend fun createPictureFile(): File {
        val user = currentUser()
        return localStore.createPictureFile(user.customerCode, user.code)
    }

    suspend fun upsertBooks(books: List<MeterBookEntity>) = dbMutex.withLock {
        val user = currentUser()
        val local = bookDao.queryBooks(user.customerCode, user.code, REPLACE_TASK_TYPE)
        val remoteIds = books.map { it.taskId }.toSet()
        local.filter { it.taskId !in remoteIds }.forEach { book ->
            bookDao.updateTaskState(book.tableId, 1)
        }
        books.forEach { incoming ->
            val existing = bookDao.queryByTaskIdAndType(
                incoming.customerCode,
                incoming.userCode,
                incoming.taskId,
                REPLACE_TASK_TYPE,
            )
            if (existing == null) {
                bookDao.insert(incoming)
            } else {
                bookDao.update(
                    existing.copy(
                        taskName = incoming.taskName,
                        taskState = 0,
                        createTime = incoming.createTime ?: existing.createTime,
                    ),
                )
            }
        }
    }

    suspend fun mergeMeters(
        taskId: String,
        incoming: List<ReplaceMeterEntity>,
        lastUpdateTime: String?,
    ) = dbMutex.withLock {
        val user = currentUser()
        val local = meterDao.queryByTask(user.customerCode, user.code, taskId)
        val remoteIds = incoming.map { it.meterId }.toSet()
        local.filter { it.meterId !in remoteIds }.forEach { meter ->
            meterDao.update(meter.copy(state = ReplaceMeterState.DELETED))
        }
        incoming.forEach { item ->
            val existing = meterDao.queryByMeterId(user.customerCode, user.code, taskId, item.meterId)
            if (existing == null) {
                meterDao.insert(item)
            } else if (existing.state == ReplaceMeterState.PENDING) {
                meterDao.update(
                    existing.copy(
                        address = item.address,
                        sort = item.sort,
                        isReplace = item.isReplace,
                        replaceRyFlux = item.replaceRyFlux ?: existing.replaceRyFlux,
                        extInfo = item.extInfo ?: existing.extInfo,
                    ),
                )
            } else {
                meterDao.update(
                    existing.copy(
                        state = ReplaceMeterState.SYNCED,
                        progress = item.progress,
                        newMeterCode = item.newMeterCode,
                        newReading = item.newReading.orEmpty(),
                        oldReading = item.oldReading.orEmpty(),
                        isReplace = item.isReplace,
                        sort = item.sort,
                        address = item.address,
                        longitude = item.longitude,
                        latitude = item.latitude,
                        caliber = item.caliber ?: existing.caliber,
                        installType = item.installType ?: existing.installType,
                        verifyOrg = item.verifyOrg ?: existing.verifyOrg,
                        verifyDate = item.verifyDate ?: existing.verifyDate,
                        verifyExpireDate = item.verifyExpireDate ?: existing.verifyExpireDate,
                        replaceRyFlux = item.replaceRyFlux ?: existing.replaceRyFlux,
                        extInfo = item.extInfo ?: existing.extInfo,
                    ),
                )
            }
        }
        if (incoming.isEmpty()) {
            local.forEach { meterDao.update(it.copy(state = ReplaceMeterState.DELETED)) }
        }
        val book = bookDao.queryByTaskIdAndType(user.customerCode, user.code, taskId, REPLACE_TASK_TYPE)
        if (book != null) {
            bookDao.updateFingerprint(
                tableId = book.tableId,
                fingerprint = lastUpdateTime,
                lastUpdateTime = lastUpdateTime,
                downloadTime = System.currentTimeMillis(),
            )
        }
    }

    suspend fun markMetersDeleted(taskId: String) = dbMutex.withLock {
        val user = currentUser()
        meterDao.queryByTask(user.customerCode, user.code, taskId).forEach {
            meterDao.update(it.copy(state = ReplaceMeterState.DELETED))
        }
    }

    suspend fun mapCounts(taskId: String): Triple<Int, Int, Int> {
        val user = currentUser()
        val unfinished = meterDao.countUnfinishedReplaceable(user.customerCode, user.code, taskId)
        val finished = meterDao.countFinishedReplaceable(user.customerCode, user.code, taskId)
        val notNeed = meterDao.countNotNeedReplace(user.customerCode, user.code, taskId)
        return Triple(unfinished, finished, notNeed)
    }

    suspend fun updateCoordinates(oldMeterCode: String, latitude: Double, longitude: Double) = dbMutex.withLock {
        val user = currentUser()
        meterDao.updateCoordinates(user.customerCode, user.code, oldMeterCode, latitude, longitude)
    }

    suspend fun clearCurrentAccount() = dbMutex.withLock {
        val user = currentUser()
        meterDao.deleteByAccount(user.customerCode, user.code)
    }
}

fun computeProgress(oldReading: String?, newMeterCode: String?): Int {
    val hasOld = !oldReading.isNullOrBlank()
    val hasNew = !newMeterCode.isNullOrBlank()
    return when {
        hasOld && hasNew -> ReplaceProgress.BOTH
        hasOld -> ReplaceProgress.OLD_DONE
        hasNew -> ReplaceProgress.NEW_DONE
        else -> ReplaceProgress.NONE
    }
}
