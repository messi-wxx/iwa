package com.cq.iwa.feature.readmeter.sync

data class SyncProgress(
    val title: String = "同步数据中",
    val taskName: String = "",
    val totalProgress: String = "",
    val tip: String = "准备同步",
    val percent: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val running: Boolean = false,
    val finished: Boolean = false,
    val catalogOnly: Boolean = false,
    val errors: List<String> = emptyList(),
) {
    val message: String
        get() = when {
            catalogOnly && finished && errors.isEmpty() -> "已获取表册名单，请勾选后同步"
            finished && errors.isEmpty() -> "同步成功"
            finished -> "同步完成，部分失败"
            tip.isNotBlank() -> tip
            else -> title
        }
}

data class SyncRequest(
    val taskIds: List<String>,
    val includeNetMeter: Boolean = false,
)
