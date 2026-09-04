package com.cq.iwa.replacemeter

import android.content.Context
import android.content.Intent

object ReplaceMeterNavigator {

    const val EXTRA_TABLE_ID = "tableId"
    const val EXTRA_TASK_ID = "taskId"

    fun openBookList(context: Context) {
        context.startActivity(Intent(context, ReplaceBookListActivity::class.java))
    }

    fun openMeterList(context: Context, taskId: String) {
        context.startActivity(
            Intent(context, ReplaceMeterListActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId),
        )
    }

    fun openDetail(context: Context, tableId: Long) {
        context.startActivity(
            Intent(context, ReplaceMeterDetailActivity::class.java)
                .putExtra(EXTRA_TABLE_ID, tableId),
        )
    }

    fun openMap(context: Context, taskId: String) {
        context.startActivity(
            Intent(context, ReplaceMapActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId),
        )
    }
}
