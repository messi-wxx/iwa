package com.cq.iwa.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cq.iwa.core.database.entity.ReadMeterEntity

@Dao
interface ReadMeterDao : BaseDao<ReadMeterEntity> {

    @Query(
        "SELECT * FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 ORDER BY sort ASC, tableId ASC",
    )
    suspend fun queryByTask(customerCode: String, userCode: String, taskId: String): List<ReadMeterEntity>

    @Query(
        "SELECT * FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId ORDER BY sort ASC, tableId ASC",
    )
    suspend fun queryAllByTask(customerCode: String, userCode: String, taskId: String): List<ReadMeterEntity>

    @Query(
        "SELECT * FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = :state ORDER BY sort ASC",
    )
    suspend fun queryByTaskAndState(
        customerCode: String,
        userCode: String,
        taskId: String,
        state: Int,
    ): List<ReadMeterEntity>

    @Query("SELECT * FROM read_meter WHERE tableId = :tableId LIMIT 1")
    suspend fun queryByTableId(tableId: Long): ReadMeterEntity?

    @Query(
        "SELECT COUNT(*) FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1",
    )
    suspend fun countVisible(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT COUNT(*) FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = :state",
    )
    suspend fun countByState(customerCode: String, userCode: String, taskId: String, state: Int): Int

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = 0
          AND (sort > :sort OR (sort = :sort AND tableId > :tableId))
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY sort ASC, tableId ASC
        LIMIT 1
        """,
    )
    suspend fun queryNextUnread(
        customerCode: String,
        userCode: String,
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String,
    ): ReadMeterEntity?

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = 0
          AND (sort < :sort OR (sort = :sort AND tableId < :tableId))
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY sort DESC, tableId DESC
        LIMIT 1
        """,
    )
    suspend fun queryPreviousUnread(
        customerCode: String,
        userCode: String,
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String,
    ): ReadMeterEntity?

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > 0 AND readTime > 0
          AND (:beforeTime = 0 OR readTime < :beforeTime)
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY readTime DESC
        LIMIT 1
        """,
    )
    suspend fun queryLastRead(
        customerCode: String,
        userCode: String,
        taskId: String,
        beforeTime: Long,
        groupName: String,
    ): ReadMeterEntity?

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND state > -1
          AND (meterCode LIKE :key OR clientCode LIKE :key OR clientName LIKE :key OR address LIKE :key)
        LIMIT 20
        """,
    )
    suspend fun search(customerCode: String, userCode: String, key: String): List<ReadMeterEntity>

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1
          AND (meterCode LIKE :key OR clientCode LIKE :key OR clientName LIKE :key OR address LIKE :key)
        LIMIT 20
        """,
    )
    suspend fun searchInTask(
        customerCode: String,
        userCode: String,
        taskId: String,
        key: String,
    ): List<ReadMeterEntity>

    @Query(
        "SELECT * FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode AND meterCode = :meterCode AND state > -1 LIMIT 1",
    )
    suspend fun queryByMeterCode(customerCode: String, userCode: String, meterCode: String): ReadMeterEntity?

    @Query(
        "UPDATE read_meter SET latitude = :latitude, longitude = :longitude WHERE customerCode = :customerCode AND userCode = :userCode AND meterCode = :meterCode AND state > -1",
    )
    suspend fun updateCoordinates(
        customerCode: String,
        userCode: String,
        meterCode: String,
        latitude: Double,
        longitude: Double,
    )

    @Query("DELETE FROM read_meter WHERE customerCode = :customerCode AND userCode = :userCode")
    suspend fun deleteByAccount(customerCode: String, userCode: String)

    @Query("DELETE FROM read_meter WHERE tableId = :tableId")
    suspend fun deleteByTableId(tableId: Long): Int

    @Query(
        """
        SELECT DISTINCT groupName FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId
          AND state > -1 AND groupName IS NOT NULL AND groupName != ''
        ORDER BY groupName ASC
        """,
    )
    suspend fun queryGroups(
        customerCode: String,
        userCode: String,
        taskId: String,
    ): List<String>

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY sort ASC, tableId ASC
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun queryPaged(
        customerCode: String,
        userCode: String,
        taskId: String,
        groupName: String,
        pageSize: Int,
        offset: Int,
    ): List<ReadMeterEntity>

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = :state
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY sort ASC, tableId ASC
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun queryPagedByState(
        customerCode: String,
        userCode: String,
        taskId: String,
        state: Int,
        groupName: String,
        pageSize: Int,
        offset: Int,
    ): List<ReadMeterEntity>

    @Query(
        """
        SELECT * FROM read_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > 0
          AND (:groupName = '' OR groupName = :groupName)
        ORDER BY readTime DESC
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun queryPagedRead(
        customerCode: String,
        userCode: String,
        taskId: String,
        groupName: String,
        pageSize: Int,
        offset: Int,
    ): List<ReadMeterEntity>
}
