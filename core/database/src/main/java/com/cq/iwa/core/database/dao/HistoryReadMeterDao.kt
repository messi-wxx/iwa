package com.cq.iwa.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cq.iwa.core.database.entity.HistoryReadMeterEntity

@Dao
interface HistoryReadMeterDao : BaseDao<HistoryReadMeterEntity> {

    @Query(
        "SELECT * FROM history_read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND meterId = :meterId ORDER BY readTime DESC",
    )
    suspend fun queryByMeter(
        customerCode: String,
        userCode: String,
        taskId: String,
        meterId: Int,
    ): List<HistoryReadMeterEntity>

    @Query("DELETE FROM history_read_meter WHERE customerCode = :customerCode AND userCode = :userCode")
    suspend fun deleteByAccount(customerCode: String, userCode: String)
}
