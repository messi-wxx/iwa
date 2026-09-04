package com.cq.iwa.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cq.iwa.core.database.entity.MeterBookEntity

@Dao
interface MeterBookDao : BaseDao<MeterBookEntity> {

    @Query(
        "SELECT * FROM meter_book WHERE customerCode = :customerCode AND userCode = :userCode AND taskType = :taskType ORDER BY taskName ASC",
    )
    suspend fun queryBooks(customerCode: String, userCode: String, taskType: Int = 1): List<MeterBookEntity>

    @Query(
        "SELECT * FROM meter_book WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId LIMIT 1",
    )
    suspend fun queryByTaskId(customerCode: String, userCode: String, taskId: String): MeterBookEntity?

    @Query(
        "SELECT * FROM meter_book WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND taskType = :taskType LIMIT 1",
    )
    suspend fun queryByTaskIdAndType(
        customerCode: String,
        userCode: String,
        taskId: String,
        taskType: Int,
    ): MeterBookEntity?

    @Query("UPDATE meter_book SET fingerprint = :fingerprint, lastUpdateTime = :lastUpdateTime, downloadTime = :downloadTime WHERE tableId = :tableId")
    suspend fun updateFingerprint(
        tableId: Long,
        fingerprint: String?,
        lastUpdateTime: String?,
        downloadTime: Long,
    )

    @Query("UPDATE meter_book SET taskState = :taskState WHERE tableId = :tableId")
    suspend fun updateTaskState(tableId: Long, taskState: Int)

    @Query("DELETE FROM meter_book WHERE customerCode = :customerCode AND userCode = :userCode")
    suspend fun deleteByAccount(customerCode: String, userCode: String)
}
