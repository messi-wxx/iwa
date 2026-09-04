package com.cq.iwa.feature.urgepayment.network

import kotlinx.serialization.Serializable

@Serializable
data class UrgeTaskDto(
    val id: Int = 0,
    val taskName: String? = null,
    val lastUpdateTime: String? = null,
    val totalFeeCount: Int? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalFee: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalQty: String? = null,
)

@Serializable
data class UrgeMeterDto(
    val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val taskId: String? = null,
    val clientCode: String? = null,
    val clientName: String? = null,
    val meterCode: String? = null,
    val address: String? = null,
    val totalFeeCount: Int? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalFee: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalQty: String? = null,
)

@Serializable
data class UrgeClientSearchDto(
    val value: List<UrgeClientInfoDto> = emptyList(),
)

@Serializable
data class UrgeClientInfoDto(
    val id: Int = 0,
    val clientKindDesc: String? = null,
    val code: String? = null,
    val name: String? = null,
    val address: String? = null,
    val cellPhone: String? = null,
    val stateDesc: String? = null,
    val openDate: String? = null,
    val remark: String? = null,
)

@Serializable
data class UrgeFeeClientDto(
    val clientId: Int = 0,
    val clientInfo: UrgeClientInfoDto? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val balance: String? = null,
    val devices: List<UrgeDeviceDto> = emptyList(),
)

@Serializable
data class UrgeDeviceDto(
    val deviceId: Int = 0,
    val deviceCode: String? = null,
    val brandName: String? = null,
    val caliberDesc: String? = null,
    val meterTypeDesc: String? = null,
    val deviceStateDesc: String? = null,
    val deviceUsageStateDesc: String? = null,
    val chargeWayDesc: String? = null,
    val valveTypeDesc: String? = null,
    val valveStateDesc: String? = null,
    val deviceFeeKindNameDesc: String? = null,
    val deviceFeeStateDesc: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val lastEndQty: String? = null,
    val lastReadDate: String? = null,
    val fullBookName: String? = null,
    val readerRemark: String? = null,
)

@Serializable
data class UrgeFeeDetailDto(
    val deviceAndSystemModuleName: String? = null,
    val feeKindName: String? = null,
    val theMonth: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val beginQtyStr: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val endQtyStr: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val normalQtyStr: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val oldDeviceQtyStr: String? = null,
    val lateFee: Float? = null,
    val receivableFee: Float? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val actualFee: String? = null,
    val lateDays: Int? = null,
)

@Serializable
data class UrgeSortClientDto(
    val code: String? = null,
)

@Serializable
data class UrgeReadListDto(
    val value: List<UrgeReadInfoDto> = emptyList(),
)

@Serializable
data class UrgeReadInfoDto(
    val meterCode: String? = null,
    val source: String? = null,
    val auditUserName: String? = null,
    val readUserName: String? = null,
    val readTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val reading: String? = null,
)

@Serializable
data class ChangePhoneBodyDto(
    val CellPhone: String,
    val Reason: String = "移动端更改联系电话",
)
