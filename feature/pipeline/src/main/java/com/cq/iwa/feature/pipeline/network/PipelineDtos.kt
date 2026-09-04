package com.cq.iwa.feature.pipeline.network

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class PipelineFollowDeviceDto(
    val appUserFocusSiteId: Int = 0,
    val sort: Int = 0,
    val siteId: Int = 0,
    val siteCode: String = "",
    val siteName: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val address: String? = null,
    val iotId: String = "",
    val siteType: Int = 0,
    val parentId: Int = 0,
    val siteMetrics: List<PipelineMetricDto> = emptyList(),
)

@Serializable
data class PipelineMetricDto(
    val id: Int = 0,
    val siteId: Int = 0,
    val sort: Int = 0,
    val metricId: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val name: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val unit: String? = null,
    val digit: Int = 0,
    val sourceSiteId: Int = 0,
    val sourceIotId: String = "",
    val sourceSiteType: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    var value: String? = null,
    var follow: Boolean = false,
    @Serializable(with = FlexibleLongSerializer::class)
    var timestamp: Long = 0,
)

@Serializable
data class PipelineTreeItemDto(
    val id: Int = 0,
    val iotId: String = "",
    val label: String = "",
    val type: Int = 0,
    val fullName: String = "",
    val children: List<PipelineTreeItemDto> = emptyList(),
)

@Serializable
data class PipelineSiteInfoDto(
    val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val iotId: String? = null,
    val gisId: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val address: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val kindName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val siteCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val siteName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val contact: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val mobile: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val phone: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val remark: String? = null,
    val isGateway: Boolean? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val createTime: String? = null,
    val isEnabled: Int = 0,
    val isFocus: Int = 0,
    val sort: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val siteTypeDesc: String? = null,
    val siteType: Int = 0,
    val profileId: String = "",
) {
    val displayCreateTime: String
        get() = createTime.orEmpty().replace("T", " ")
}

@Serializable
data class PipelineMonitorDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val ts: Long = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    var value: String? = null,
)

@Serializable
data class PipelineProfileDto(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class PipelineSubscribeParam(
    var tsSubCmds: List<PipelineTsSubCmd> = emptyList(),
)

@Serializable
data class PipelineTsSubCmd(
    val cmdId: Int = 0,
    val entityType: String = "",
    val entityId: String = "",
    val keys: String = "",
    val scope: String = "LATEST_TELEMETRY",
)

@Serializable
data class PipelineHistoryParam(
    val deviceId: String,
    val beginTime: String,
    val endTime: String,
    val keys: List<String>,
    val deviceType: Int,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class PipelineRecordParam(
    val iotId: String = "",
    val alarmName: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val entityType: Int? = null,
    val severityList: List<String> = emptyList(),
    val statusList: List<Int> = emptyList(),
    val startTime: String? = null,
    val endTime: String? = null,
)

@Serializable
data class PipelineAlarmRecordResultDto(
    val value: List<PipelineAlarmRecordDto> = emptyList(),
)

@Serializable
data class PipelineAlarmRecordDto(
    val id: String = "",
    val createdTime: String = "",
    val originatorName: String = "",
    val name: String = "",
    val serverityName: String = "",
    val isCleared: Boolean = false,
    val isAcknowledged: Boolean = false,
    val startTime: String = "",
    val durationTime: String = "",
    val details: String = "",
    val unionStatuName: String = "",
)

@Serializable
data class PipelineAlarmMessageDto(
    val cmdId: Int = 0,
    val data: PipelineAlarmDataWrap? = null,
)

@Serializable
data class PipelineAlarmDataWrap(
    val data: List<PipelineAlarmPushDto> = emptyList(),
)

@Serializable
data class PipelineAlarmPushDto(
    val type: String = "",
    @Serializable(with = FlexibleLongSerializer::class)
    val createdTime: Long = 0,
    val originatorName: String = "",
    val id: PipelineAlarmIdDto = PipelineAlarmIdDto(),
)

@Serializable
data class PipelineAlarmIdDto(
    val id: String = "",
)
