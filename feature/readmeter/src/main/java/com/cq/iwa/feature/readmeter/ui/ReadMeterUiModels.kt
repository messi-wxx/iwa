package com.cq.iwa.feature.readmeter.ui

data class BookItemUi(
    val taskId: String,
    val taskName: String,
    val lastUpdateTime: String?,
    val taskState: Int,
    val total: Int,
    val unread: Int,
    val read: Int,
    val selected: Boolean = false,
    val downloaded: Boolean = false,
)

data class MeterItemUi(
    val tableId: Long,
    val meterCode: String,
    val clientName: String,
    val address: String,
    val state: Int,
    val sort: Int,
    val reading: String,
)

data class BookListUi(
    val platform: String,
    val isBcp: Boolean,
    val selecting: Boolean,
    val includeNetMeter: Boolean,
    val books: List<BookItemUi>,
    val tick: Int = 0,
)

data class MeterListUi(
    val taskName: String,
    val meters: List<MeterItemUi>,
    val filter: MeterFilter = MeterFilter.ALL,
    val groups: List<String> = emptyList(),
    val groupName: String = "",
    val hasMore: Boolean = false,
    val tick: Int = 0,
)

enum class MeterFilter { ALL, UNREAD, READ }

data class MeterDetailUi(
    val tableId: Long,
    val meterId: Int,
    val taskId: String,
    val meterCode: String,
    val caliber: String,
    val address: String,
    val clientName: String,
    val clientCode: String,
    val reading: String,
    val remark: String,
    val lastRead: String,
    val photos: List<String>,
    val envPhotos: List<String>,
    val extInfo: List<Pair<String, String>>,
    val moreInfo: String,
    val hasExtInfo: Boolean,
    val phone: String,
    val usageText: String,
    val forceNfc: Boolean,
    val calculateUsage: Boolean,
    val showEnvironmentView: Boolean,
    val nfcUnlocked: Boolean,
)
