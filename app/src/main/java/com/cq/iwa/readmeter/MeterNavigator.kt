package com.cq.iwa.readmeter

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.readmeter.MeterPlatform

object MeterNavigator {

    const val EXTRA_TABLE_ID = "tableId"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_PLATFORM = "platform"
    const val EXTRA_FROM_NFC = "fromNfc"

    fun openBookList(context: Context) {
        context.startActivity(Intent(context, BookListActivity::class.java))
    }

    fun openMeterList(context: Context, taskId: String, platform: String) {
        context.startActivity(
            Intent(context, MeterListActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_PLATFORM, platform),
        )
    }

    fun openDetail(context: Context, tableId: Long, platform: String, fromNfc: Boolean = false) {
        val clazz = if (MeterPlatform.fromConfig(platform) == MeterPlatform.BCP) {
            BcpMeterDetailActivity::class.java
        } else {
            InternalMeterDetailActivity::class.java
        }
        context.startActivity(
            Intent(context, clazz)
                .putExtra(EXTRA_TABLE_ID, tableId)
                .putExtra(EXTRA_PLATFORM, platform)
                .putExtra(EXTRA_FROM_NFC, fromNfc),
        )
    }
}
