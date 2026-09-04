package com.cq.iwa.feature.installation.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class InstListRequest(
    val forData: Boolean = false,
    @Serializable(with = FlexibleStringSerializer::class)
    val code: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val address: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val applicantInfo: String? = null,
    val source: List<Int> = emptyList(),
    val type: List<Int> = emptyList(),
    val state: List<Int> = emptyList(),
    val taskName: List<String> = emptyList(),
    @Serializable(with = FlexibleStringSerializer::class)
    val beginTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val endTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val workBenchCode: String? = null,
)

@Serializable
data class InstListResultDto(
    val key: InstPageKeyDto = InstPageKeyDto(),
    val value: List<InstProjectDto> = emptyList(),
)

@Serializable
data class InstPageKeyDto(
    val pageSize: Int = 0,
    val currentPage: Int = 0,
    val pageTotal: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val extraInfo: String? = null,
)

@Serializable
data class InstProjectDto(
    val id: Int = 0,
    val code: String = "",
    val state: Int = 0,
    val type: Int = 0,
    val gisId: Int = 0,
    val source: Int = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val cardNo: String = "",
    val processInstanceId: String = "",
    val createTime: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val sourceDesc: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val typeDesc: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val stateDesc: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val taskName: String? = null,
    var taskId: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val taskDefId: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val processDefinitionId: String? = null,
    val isFollow: Boolean = false,
) {
    val formattedCreateTime: String
        get() = createTime.replace("T", " ")
}

@Serializable
data class InstQtyResultDto(
    val title_Handler: List<InstQtyItemDto> = emptyList(),
    val title_Initiator: List<InstQtyItemDto> = emptyList(),
    val title_Completer: List<InstQtyItemDto> = emptyList(),
)

@Serializable
data class InstQtyItemDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val label: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val code: String? = null,
    val value: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val url: String? = null,
)

@Serializable
data class InstTaskDetailDto(
    val config: InstTaskConfigDto? = null,
    val detail: InstTaskDetailInnerDto? = null,
)

@Serializable
data class InstTaskDetailInnerDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val assignee: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val lastFormData: String? = null,
)

@Serializable
data class InstTaskConfigDto(
    val taskConfigId: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val flowVarFields: String? = null,
    val actionConfig: List<InstActionConfigDto> = emptyList(),
    val form: InstFormDto? = null,
    val claimMode: Int = 0,
)

@Serializable
data class InstActionConfigDto(
    val type: String = "",
    val label: String = "",
)

@Serializable
data class InstFormDto(
    val id: Int = 0,
    val name: String = "",
    val jsonSchema: String = "",
    val mobileJsonSchema: String = "",
)

@Serializable
data class InstProcessDetailDto(
    val historyProcNodeList: List<InstHistoryNodeDto> = emptyList(),
    val activeProcNodeList: List<InstActiveGroupDto> = emptyList(),
    val historyProcNodeGroupList: List<InstActiveGroupDto> = emptyList(),
    val processFormList: List<InstProcessFormDto> = emptyList(),
    val startUserName: String = "",
)

@Serializable
data class InstProcessFormDto(
    val taskId: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val formData: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val type: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val formMobileJsonSchema: String? = null,
)

@Serializable
data class InstActiveGroupDto(
    val nodeDefKey: String = "",
    val nodeDefName: String = "",
    val childNodeList: List<InstChildNodeDto> = emptyList(),
    @Serializable(with = FlexibleStringSerializer::class)
    val groupStatus: String? = null,
)

@Serializable
data class InstChildNodeDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val insId: String? = null,
    val type: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val nodeDefKey: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val nodeDefName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val createTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val endTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val duration: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val assignName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val candidateGroupsName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val candidateUsersName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val dueDate: String? = null,
    val isOverdue: Boolean = false,
    val commentList: List<InstCommentDto> = emptyList(),
)

@Serializable
data class InstHistoryNodeDto(
    val insId: String = "",
    val type: String = "",
    val nodeDefName: String = "",
    val createTime: String = "",
    val endTime: String = "",
    val assignName: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val candidateGroupsName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val candidateUsersName: String? = null,
    val dueDate: String = "",
    val duration: String = "",
    val isOverdue: Boolean = false,
    val commentList: List<InstCommentDto> = emptyList(),
)

@Serializable
data class InstCommentDto(
    val id: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val message: String? = null,
    val userId: String = "",
    val createTime: String = "",
    val type: String = "",
    val userName: String = "",
)

@Serializable
data class InstSketchResultDto(
    val sketchFieldsList: List<InstSketchDto> = emptyList(),
    val dataList: List<InstTableDto> = emptyList(),
    val isFollow: Boolean? = null,
)

@Serializable
data class InstSketchDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val key: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val label: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val type: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val value: String? = null,
    val isConvert: Boolean? = null,
)

@Serializable
data class InstTableDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val tableName: String? = null,
    val queryFields: List<InstFieldDto> = emptyList(),
    val datas: List<JsonObject> = emptyList(),
)

@Serializable
data class InstFieldDto(
    val displayName: String = "",
    val showField: String = "",
    val dataType: String = "",
)

@Serializable
data class InstRejectTargetDto(
    val taskDefKey: String = "",
    val name: String = "",
    val canReject: Boolean = false,
)

@Serializable
data class InstRejectBody(
    val reason: String,
    val taskId: String,
    val targetActivityId: String,
)

@Serializable
data class InstExtendBody(
    val dueDate: String,
    val reason: String,
    val taskId: String,
)

@Serializable
data class InstUrgeBody(
    val projectId: Int,
    val urgeContent: String,
)

@Serializable
data class InstOverviewDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val xml: String? = null,
    val highlightedNodeIds: List<String> = emptyList(),
    val activeNodeIds: List<String> = emptyList(),
)

@Serializable
data class InstRecordInfoBody(
    @Serializable(with = FlexibleStringSerializer::class)
    val address: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val meterNo: String? = null,
    val projectId: Int,
    @Serializable(with = FlexibleStringSerializer::class)
    val userNo: String? = null,
    val forData: Boolean = false,
)

@Serializable
data class InstMeterRecordResultDto(
    val key: InstPageKeyDto = InstPageKeyDto(),
    val value: List<InstMeterRecordDto> = emptyList(),
)

@Serializable
data class InstMeterRecordDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val meterNo: String = "",
    val userNo: String = "",
    val address: String = "",
    val initWater: Int = 0,
    val caliber: String = "",
    val type: String = "",
    val factory: String = "",
    val direction: String = "",
    val projectCode: String = "",
    val projectName: String = "",
    val projectAddress: String = "",
    val projectPhone: String = "",
)

@Serializable
data class InstAddMeterBody(
    val address: String = "",
    val caliber: String = "",
    val direction: String = "",
    val factory: String = "",
    val id: Int = 0,
    val initWater: Int = 0,
    val meterNo: String = "",
    val projectId: Int = 0,
    val type: String = "",
    val userNo: String = "",
)

@Serializable
data class InstInstallResultDto(
    val key: InstPageKeyDto = InstPageKeyDto(),
    val value: List<InstInstallMeterDto> = emptyList(),
)

@Serializable
data class InstInstallMeterDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val direction: String = "",
    val caliber: String = "",
    val number: Int = 0,
)

@Serializable
data class InstLogRequest(
    val beginTime: String = "",
    val endTime: String = "",
    val forData: Boolean = false,
    val projectId: Int = 0,
    val type: List<Int> = emptyList(),
)

@Serializable
data class InstLogResultDto(
    val key: InstPageKeyDto = InstPageKeyDto(),
    val value: List<InstLogDto> = emptyList(),
)

@Serializable
data class InstLogDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val typeDesc: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val content: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val createTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val createByName: String? = null,
)

@Serializable
data class InstProjectIdBody(
    val projectId: Int,
)

@Serializable
data class InstDocumentDownBody(
    val projectId: Int,
    val type: Int,
)

sealed class InstTimelineItem {
    abstract val nodeDefName: String

    data class ActiveNode(
        override val nodeDefName: String,
        val childNodeList: List<InstChildNodeDto>,
    ) : InstTimelineItem()

    data class EndEventNode(
        override val nodeDefName: String,
        val createTime: String?,
    ) : InstTimelineItem()

    data class HistoryGroupNode(
        override val nodeDefName: String,
        val groupStatus: String?,
        val childNodeList: List<InstChildNodeDto>,
    ) : InstTimelineItem()

    data class HistoryNode(
        override val nodeDefName: String,
        val type: String,
        val childNode: InstChildNodeDto,
    ) : InstTimelineItem()
}
