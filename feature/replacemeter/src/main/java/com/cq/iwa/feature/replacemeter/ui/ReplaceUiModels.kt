package com.cq.iwa.feature.replacemeter.ui

data class ReplaceBookItemUi(
    val taskId: String,
    val taskName: String,
    val lastUpdateTime: String?,
    val taskState: Int,
    val total: Int,
    val unfinished: Int,
    val finished: Int,
    val selected: Boolean = false,
    val downloaded: Boolean = false,
)

data class ReplaceBookListUi(
    val platform: String,
    val isEdc: Boolean,
    val selecting: Boolean,
    val books: List<ReplaceBookItemUi>,
    val tick: Int = 0,
)

enum class ReplaceMeterFilter { UNFINISHED, FINISHED }

data class ReplaceMeterItemUi(
    val tableId: Long,
    val oldMeterCode: String,
    val address: String,
    val clientCode: String,
    val oldReading: String,
    val newMeterCode: String,
    val newReading: String,
    val isReplace: Int,
    val progress: Int,
    val state: Int,
    val sort: Int,
    val extInfo: String,
)

data class ReplaceMeterListUi(
    val taskName: String,
    val meters: List<ReplaceMeterItemUi>,
    val filter: ReplaceMeterFilter,
    val groups: List<String>,
    val groupName: String,
    val unfinished: Int,
    val finished: Int,
    val hasMore: Boolean = false,
    val tick: Int = 0,
)

data class ReplaceMeterDetailUi(
    val tableId: Long,
    val taskId: String,
    val address: String,
    val placeAddress: String,
    val clientCode: String,
    val oldMeterCode: String,
    val oldReading: String,
    val replaceRyFlux: String,
    val newMeterCode: String,
    val newReading: String,
    val caliber: String,
    val oldPhoto: String,
    val newPhoto: String,
    val envPhotos: List<String>,
    val verifyOrg: String,
    val verifyDate: String,
    val verifyExpireDate: String,
    val installType: String,
    val showOld: Boolean,
    val showNew: Boolean,
    val needPosition: Boolean,
    val nfcUnlocked: Boolean = true,
)
