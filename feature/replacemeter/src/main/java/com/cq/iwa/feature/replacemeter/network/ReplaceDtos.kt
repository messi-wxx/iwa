package com.cq.iwa.feature.replacemeter.network

import kotlinx.serialization.Serializable
import com.cq.iwa.feature.readmeter.network.FlexibleStringSerializer

@Serializable
data class ReplaceBookDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,
    val taskName: String = "",
    val lastUpdateTime: String? = null,
    val createTime: String? = null,
    val taskType: Int = 2,
)

@Serializable
data class ReplaceMeterDto(
    val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val taskId: String? = null,
    val clientCode: String? = null,
    val customerCode: String? = null,
    val address: String? = null,
    val caliber: Int? = null,
    val oldMeterCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val oldReading: String? = null,
    val newMeterCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val newReading: String? = null,
    val replaceName: String? = null,
    val sort: Int = 0,
    val isReplace: Int = 1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @Serializable(with = FlexibleStringSerializer::class)
    val replaceRYFlux: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val extInfo: String? = null,
    val installType: String? = null,
    val verifyOrg: String? = null,
    val verifyDate: String? = null,
    val verifyExpireDate: String? = null,
)

@Serializable
data class ReplaceModelDto(
    val id: Int,
    val taskId: String? = null,
    val oldReading: Double? = null,
    val oldPicList: List<String> = emptyList(),
    val newMeterCode: String? = null,
    val newReading: Double? = null,
    val newCaliber: Int? = null,
    val newPicList: List<String> = emptyList(),
    val installType: String? = null,
    val verifyOrg: String? = null,
    val verifyDate: String? = null,
    val verifyExpireDate: String? = null,
)

@Serializable
data class AttachmentDto(
    val attachmentId: String = "",
    val seq: Int = 0,
)

@Serializable
data class ReplaceLocationDto(
    val linkId: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)
