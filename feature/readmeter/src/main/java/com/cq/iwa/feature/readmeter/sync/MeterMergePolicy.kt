package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.feature.readmeter.MeterState
import com.cq.iwa.feature.readmeter.network.MeterDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterMergePolicy @Inject constructor() {

    fun toEntity(
        dto: MeterDto,
        taskId: String,
        customerCode: String,
        userCode: String,
        readName: String,
        local: ReadMeterEntity?,
    ): ReadMeterEntity {
        val archive = archiveFrom(dto, taskId, customerCode, userCode, readName)
        val serverReading = dto.reading?.takeIf { it.isNotBlank() }
        val serverRemark = dto.remark?.takeIf { it.isNotBlank() }
        val serverHasReading = !serverReading.isNullOrBlank() || !serverRemark.isNullOrBlank()
        if (local == null) {
            return archive.copy(
                reading = serverReading,
                remark = serverRemark,
                state = if (serverHasReading) MeterState.UPLOADED else MeterState.UNREAD,
                photos = emptyList(),
                envPhotos = emptyList(),
                readTime = 0,
            )
        }
        val keepJobGps = local.state == MeterState.READ || local.hasEnvPhotos()
        return if (local.state == MeterState.READ) {
            archive.copy(
                tableId = local.tableId,
                reading = local.reading,
                remark = local.remark,
                photos = local.photos,
                envPhotos = local.envPhotos,
                state = MeterState.READ,
                readTime = local.readTime,
                latitude = local.latitude,
                longitude = local.longitude,
            )
        } else {
            archive.copy(
                tableId = local.tableId,
                reading = serverReading,
                remark = serverRemark,
                photos = local.photos.ifEmpty { emptyList() },
                envPhotos = local.envPhotos,
                state = if (serverHasReading) MeterState.UPLOADED else MeterState.UNREAD,
                readTime = if (serverHasReading) local.readTime else 0,
                latitude = if (keepJobGps) local.latitude else dto.latitude.takeIf { it != 0.0 } ?: local.latitude,
                longitude = if (keepJobGps) local.longitude else dto.longitude.takeIf { it != 0.0 } ?: local.longitude,
            )
        }
    }

    private fun archiveFrom(
        dto: MeterDto,
        taskId: String,
        customerCode: String,
        userCode: String,
        readName: String,
    ): ReadMeterEntity {
        return ReadMeterEntity(
            meterId = dto.id,
            taskId = taskId,
            customerCode = customerCode,
            userCode = userCode,
            readName = dto.readName ?: readName,
            meterCode = dto.meterCode,
            address = dto.address,
            caliber = dto.caliber,
            clientName = dto.clientName,
            clientCode = dto.clientCode,
            cellPhone = dto.cellPhone,
            lastRead = dto.lastRead,
            sort = dto.sort,
            groupName = dto.groupName,
            extInfo = dto.extInfoMap(),
            latitude = dto.latitude,
            longitude = dto.longitude,
        )
    }
}
