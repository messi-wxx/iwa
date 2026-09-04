package com.cq.iwa.feature.urgepayment.ui

data class UrgeTaskUi(
    val bookId: Int,
    val taskName: String,
    val feeCountText: String,
)

data class UrgeTaskListUi(
    val tasks: List<UrgeTaskUi>,
    val tick: Int = 0,
)

data class UrgeMeterUi(
    val clientCode: String,
    val title: String,
    val address: String,
    val feeText: String,
)

data class UrgeMeterListUi(
    val taskName: String,
    val meters: List<UrgeMeterUi>,
    val tick: Int = 0,
)

data class UrgeSearchItemUi(
    val clientCode: String,
    val title: String,
    val address: String,
)

data class UrgeFeeItemUi(
    val month: String,
    val meter: String,
    val startQty: String,
    val endQty: String,
    val useQty: String,
    val receivableFee: String,
    val lateFee: String,
)

data class UrgeDeviceItemUi(
    val deviceId: Int,
    val meterCode: String,
    val caliber: String,
    val feeKind: String,
    val feeState: String,
    val chargeWay: String,
    val reading: String,
    val readDate: String,
    val valveState: String,
    val useState: String,
    val bookName: String,
    val remark: String,
)

data class UrgeReadHistoryUi(
    val meterCode: String,
    val readDate: String,
    val reading: String,
    val source: String,
    val readUser: String,
    val auditUser: String,
)

data class UrgeDetailUi(
    val clientCode: String,
    val clientId: Int,
    val name: String,
    val code: String,
    val phone: String,
    val address: String,
    val state: String,
    val openDate: String,
    val remark: String,
    val balance: String,
    val oweFee: String,
    val hasFees: Boolean,
    val fees: List<UrgeFeeItemUi>,
    val hasDevices: Boolean,
    val devices: List<UrgeDeviceItemUi>,
)
