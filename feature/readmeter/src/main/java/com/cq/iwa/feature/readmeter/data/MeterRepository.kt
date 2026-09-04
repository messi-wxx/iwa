package com.cq.iwa.feature.readmeter.data

import com.cq.iwa.core.database.dao.HistoryReadMeterDao
import com.cq.iwa.core.database.dao.MeterBookDao
import com.cq.iwa.core.database.dao.ReadMeterDao
import com.cq.iwa.core.database.dao.UserConfigDao
import com.cq.iwa.core.database.entity.HistoryReadMeterEntity
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.core.database.entity.UserEntity
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.MeterState
import com.cq.iwa.feature.readmeter.ui.MeterFilter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterRepository @Inject constructor(
    private val bookDao: MeterBookDao,
    private val meterDao: ReadMeterDao,
    private val historyDao: HistoryReadMeterDao,
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

    suspend fun remarks(): List<String> {
        val fromConfig = userConfigDao.findAllByName("MobileReadRemark")
            .mapNotNull { it.configValue }
            .filter { it.isNotBlank() }
        val list = fromConfig.ifEmpty { listOf("正常抄表", "估抄", "表具污损", "房屋没人") }
        return if (list.any { it == "其它" }) list else list + "其它"
    }

    suspend fun queryBooks(): List<MeterBookEntity> {
        val user = currentUser()
        return bookDao.queryBooks(user.customerCode, user.code)
    }

    suspend fun queryBook(taskId: String): MeterBookEntity? {
        val user = currentUser()
        return bookDao.queryByTaskId(user.customerCode, user.code, taskId)
    }

    suspend fun queryVisibleMeters(taskId: String): List<ReadMeterEntity> {
        val user = currentUser()
        return meterDao.queryByTask(user.customerCode, user.code, taskId)
    }

    suspend fun queryPagedMeters(
        taskId: String,
        filter: MeterFilter,
        pageSize: Int,
        offset: Int,
        groupName: String = "",
    ): List<ReadMeterEntity> {
        val user = currentUser()
        return when (filter) {
            MeterFilter.UNREAD -> meterDao.queryPagedByState(
                user.customerCode, user.code, taskId, MeterState.UNREAD, groupName, pageSize, offset,
            )
            MeterFilter.READ -> meterDao.queryPagedRead(
                user.customerCode, user.code, taskId, groupName, pageSize, offset,
            )
            MeterFilter.ALL -> meterDao.queryPaged(
                user.customerCode, user.code, taskId, groupName, pageSize, offset,
            )
        }
    }

    suspend fun queryPending(taskId: String): List<ReadMeterEntity> {
        val user = currentUser()
        return meterDao.queryByTaskAndState(user.customerCode, user.code, taskId, MeterState.READ)
    }

    suspend fun queryMeter(tableId: Long): ReadMeterEntity? {
        val user = currentUser()
        val meter = meterDao.queryByTableId(tableId) ?: return null
        return meter.takeIf { it.customerCode == user.customerCode && it.userCode == user.code }
    }

    suspend fun queryByMeterCode(meterCode: String): ReadMeterEntity? {
        val user = currentUser()
        return meterDao.queryByMeterCode(user.customerCode, user.code, meterCode)
    }

    suspend fun search(keyword: String, taskId: String? = null): List<ReadMeterEntity> {
        val user = currentUser()
        val key = "%$keyword%"
        return if (taskId.isNullOrBlank()) {
            meterDao.search(user.customerCode, user.code, key)
        } else {
            meterDao.searchInTask(user.customerCode, user.code, taskId, key)
        }
    }

    suspend fun nextUnread(current: ReadMeterEntity, forward: Boolean, groupName: String = ""): ReadMeterEntity? {
        val user = currentUser()
        return if (forward) {
            meterDao.queryNextUnread(
                user.customerCode, user.code, current.taskId, current.sort, current.tableId, groupName,
            )
        } else {
            meterDao.queryPreviousUnread(
                user.customerCode, user.code, current.taskId, current.sort, current.tableId, groupName,
            )
        }
    }

    suspend fun lastRead(taskId: String, beforeTime: Long, groupName: String = ""): ReadMeterEntity? {
        val user = currentUser()
        return meterDao.queryLastRead(user.customerCode, user.code, taskId, beforeTime, groupName)
    }

    suspend fun queryGroups(taskId: String): List<String> {
        val user = currentUser()
        return meterDao.queryGroups(user.customerCode, user.code, taskId)
    }

    suspend fun deleteReadMeter(tableId: Long): Boolean = dbMutex.withLock {
        val user = currentUser()
        val meter = meterDao.queryByTableId(tableId) ?: return@withLock false
        if (meter.customerCode != user.customerCode || meter.userCode != user.code) return@withLock false
        if (meter.state <= MeterState.UNREAD) return@withLock false
        meterDao.deleteByTableId(tableId) == 1
    }

    suspend fun bookCounts(taskId: String): Triple<Int, Int, Int> {
        val user = currentUser()
        val total = meterDao.countVisible(user.customerCode, user.code, taskId)
        val unread = meterDao.countByState(user.customerCode, user.code, taskId, MeterState.UNREAD)
        val pending = meterDao.countByState(user.customerCode, user.code, taskId, MeterState.READ)
        val uploaded = meterDao.countByState(user.customerCode, user.code, taskId, MeterState.UPLOADED)
        return Triple(total, unread, pending + uploaded)
    }

    suspend fun <T> withDbLock(block: suspend () -> T): T = dbMutex.withLock { block() }

    suspend fun upsertBooks(books: List<MeterBookEntity>) = dbMutex.withLock {
        books.forEach { incoming ->
            val local = bookDao.queryByTaskId(incoming.customerCode, incoming.userCode, incoming.taskId)
            if (local == null) {
                bookDao.insert(incoming)
            } else {
                bookDao.update(
                    incoming.copy(
                        tableId = local.tableId,
                        fingerprint = local.fingerprint ?: incoming.fingerprint,
                        downloadTime = local.downloadTime,
                    ),
                )
            }
        }
    }

    suspend fun markMissingBooksCompleted(serverTaskIds: Set<String>) = dbMutex.withLock {
        val user = currentUser()
        bookDao.queryBooks(user.customerCode, user.code)
            .filter { it.taskId !in serverTaskIds }
            .forEach { bookDao.updateTaskState(it.tableId, 1) }
    }

    suspend fun saveFingerprint(
        book: MeterBookEntity,
        fingerprint: String?,
        lastUpdateTime: String?,
    ) = dbMutex.withLock {
        bookDao.updateFingerprint(
            tableId = book.tableId,
            fingerprint = fingerprint,
            lastUpdateTime = lastUpdateTime,
            downloadTime = System.currentTimeMillis(),
        )
    }

    suspend fun insertMeter(entity: ReadMeterEntity) = dbMutex.withLock {
        meterDao.insert(entity)
    }

    suspend fun updateMeter(entity: ReadMeterEntity) = dbMutex.withLock {
        meterDao.update(entity)
    }

    suspend fun saveReading(entity: ReadMeterEntity) = dbMutex.withLock {
        val updated = entity.copy(state = MeterState.READ, readTime = System.currentTimeMillis())
        meterDao.update(updated)
        historyDao.insert(
            HistoryReadMeterEntity(
                taskId = updated.taskId,
                meterId = updated.meterId,
                customerCode = updated.customerCode,
                userCode = updated.userCode.ifBlank { currentUser().code },
                meterCode = updated.meterCode,
                address = updated.address,
                reading = updated.reading,
                remark = updated.remark,
                photos = updated.photos,
                readTime = updated.readTime,
            ),
        )
        updated
    }

    suspend fun markUploaded(tableIds: List<Long>) = dbMutex.withLock {
        tableIds.forEach { id ->
            val meter = meterDao.queryByTableId(id) ?: return@forEach
            meterDao.update(meter.copy(state = MeterState.UPLOADED))
        }
    }

    suspend fun queryLocationPending(taskId: String): List<ReadMeterEntity> {
        return queryVisibleMeters(taskId).filter { meter ->
            meter.envPhotos.any { it.isNotBlank() && it != "button" }
        }
    }

    suspend fun clearEnvPhotos(tableIds: List<Long>) = dbMutex.withLock {
        val user = currentUser()
        tableIds.forEach { id ->
            val meter = meterDao.queryByTableId(id) ?: return@forEach
            meter.envPhotos.forEach { path ->
                localStore.resolveLocalFile(path, user.customerCode, user.code)?.delete()
            }
            meterDao.update(meter.copy(envPhotos = emptyList()))
        }
    }

    suspend fun queryLocalAll(taskId: String): List<ReadMeterEntity> {
        val user = currentUser()
        return meterDao.queryAllByTask(user.customerCode, user.code, taskId)
    }

    suspend fun updateCoordinates(meterCode: String, latitude: Double, longitude: Double) = dbMutex.withLock {
        val user = currentUser()
        meterDao.updateCoordinates(user.customerCode, user.code, meterCode, latitude, longitude)
    }

    suspend fun clearCurrentAccount() = dbMutex.withLock {
        val user = currentUser()
        bookDao.deleteByAccount(user.customerCode, user.code)
        meterDao.deleteByAccount(user.customerCode, user.code)
        historyDao.deleteByAccount(user.customerCode, user.code)
    }
}
