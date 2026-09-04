package com.cq.iwa.feature.replacemeter

object ReplaceMeterState {
    const val DELETED = -1
    const val PENDING = 1
    const val SYNCED = 2
}

object ReplaceProgress {
    const val NONE = 0
    const val OLD_DONE = 1
    const val NEW_DONE = 2
    const val BOTH = 3
}

object ReplaceShowWay {
    const val OLD_ONLY = 0
    const val NEW_ONLY = 1
    const val BOTH = 2
}

const val REPLACE_TASK_TYPE = 2
const val REPLACE_UPLOAD_BATCH = 100
