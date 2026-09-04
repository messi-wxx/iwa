package com.cq.iwa.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cq.iwa.core.database.entity.ReplaceMeterEntity

@Dao
interface ReplaceMeterDao : BaseDao<ReplaceMeterEntity> {

    @Query(
        "SELECT * FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 ORDER BY sort ASC, tableId ASC",
    )
    suspend fun queryByTask(
        customerCode: String,
        userCode: String,
        taskId: String,
    ): List<ReplaceMeterEntity>

    @Query(
        "SELECT * FROM replace_meter WHERE tableId = :tableId LIMIT 1",
    )
    suspend fun queryByTableId(tableId: Long): ReplaceMeterEntity?

    @Query(
        "SELECT * FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND meterId = :meterId LIMIT 1",
    )
    suspend fun queryByMeterId(
        customerCode: String,
        userCode: String,
        taskId: String,
        meterId: Int,
    ): ReplaceMeterEntity?

    @Query(
        "SELECT * FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state = 1 ORDER BY sort ASC",
    )
    suspend fun queryPending(
        customerCode: String,
        userCode: String,
        taskId: String,
    ): List<ReplaceMeterEntity>

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1",
    )
    suspend fun countVisible(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND progress < 3",
    )
    suspend fun countUnfinished(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND progress = 3",
    )
    suspend fun countFinished(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT * FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND (oldMeterCode LIKE :key OR address LIKE :key OR newMeterCode LIKE :key) AND state > -1 ORDER BY sort ASC",
    )
    suspend fun search(
        customerCode: String,
        userCode: String,
        key: String,
    ): List<ReplaceMeterEntity>

    @Query(
        "SELECT DISTINCT extInfo FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND extInfo IS NOT NULL AND extInfo != ''",
    )
    suspend fun queryGroups(
        customerCode: String,
        userCode: String,
        taskId: String,
    ): List<String>

    @Query(
        "SELECT * FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND newMeterCode = :newMeterCode AND tableId != :excludeTableId AND state > -1 LIMIT 1",
    )
    suspend fun queryDuplicateNewCode(
        customerCode: String,
        userCode: String,
        taskId: String,
        newMeterCode: String,
        excludeTableId: Long,
    ): ReplaceMeterEntity?

    @Query(
        """
        SELECT * FROM replace_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId
          AND state > -1 AND isReplace = 1
          AND (sort > :sort OR (sort = :sort AND tableId > :tableId))
          AND (:groupName = '' OR extInfo = :groupName)
        ORDER BY sort ASC, tableId ASC
        LIMIT 1
        """,
    )
    suspend fun queryNext(
        customerCode: String,
        userCode: String,
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String,
    ): ReplaceMeterEntity?

    @Query(
        """
        SELECT * FROM replace_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId
          AND state > -1 AND isReplace = 1
          AND (sort < :sort OR (sort = :sort AND tableId < :tableId))
          AND (:groupName = '' OR extInfo = :groupName)
        ORDER BY sort DESC, tableId DESC
        LIMIT 1
        """,
    )
    suspend fun queryPrevious(
        customerCode: String,
        userCode: String,
        taskId: String,
        sort: Int,
        tableId: Long,
        groupName: String,
    ): ReplaceMeterEntity?

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND isReplace = 1 AND progress < 3",
    )
    suspend fun countUnfinishedReplaceable(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND isReplace = 1 AND progress = 3",
    )
    suspend fun countFinishedReplaceable(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "SELECT COUNT(*) FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1 AND isReplace = 0",
    )
    suspend fun countNotNeedReplace(customerCode: String, userCode: String, taskId: String): Int

    @Query(
        "UPDATE replace_meter SET latitude = :latitude, longitude = :longitude WHERE customerCode = :customerCode AND userCode = :userCode AND oldMeterCode = :oldMeterCode AND state > -1",
    )
    suspend fun updateCoordinates(
        customerCode: String,
        userCode: String,
        oldMeterCode: String,
        latitude: Double,
        longitude: Double,
    )

    @Query("DELETE FROM replace_meter WHERE customerCode = :customerCode AND userCode = :userCode")
    suspend fun deleteByAccount(customerCode: String, userCode: String)

    @Query(
        """
        SELECT * FROM replace_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1
          AND (:groupName = '' OR extInfo = :groupName)
          AND ((:finished = 0 AND progress < 3) OR (:finished = 1 AND progress = 3))
        ORDER BY sort ASC, tableId ASC
        LIMIT :pageSize OFFSET :offset
        """,
    )
    suspend fun queryPaged(
        customerCode: String,
        userCode: String,
        taskId: String,
        groupName: String,
        finished: Int,
        pageSize: Int,
        offset: Int,
    ): List<ReplaceMeterEntity>

    @Query(
        """
        SELECT COUNT(*) FROM replace_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1
          AND (:groupName = '' OR extInfo = :groupName) AND progress < 3
        """,
    )
    suspend fun countUnfinishedByGroup(
        customerCode: String,
        userCode: String,
        taskId: String,
        groupName: String,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM replace_meter
        WHERE customerCode = :customerCode AND userCode = :userCode AND taskId = :taskId AND state > -1
          AND (:groupName = '' OR extInfo = :groupName) AND progress = 3
        """,
    )
    suspend fun countFinishedByGroup(
        customerCode: String,
        userCode: String,
        taskId: String,
        groupName: String,
    ): Int
}
