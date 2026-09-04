package com.cq.iwa.pipeline

import android.content.Context
import android.content.Intent

object PipelineNavigator {
    const val EXTRA_SITE_ID = "id"
    const val EXTRA_IOT_ID = "iotId"
    const val EXTRA_SITE_TYPE = "siteType"

    fun openFollow(context: Context) {
        context.startActivity(Intent(context, PipelineFollowActivity::class.java))
    }

    fun openTree(context: Context) {
        context.startActivity(Intent(context, PipelineTreeActivity::class.java))
    }

    fun openDetail(context: Context, siteId: Int) {
        context.startActivity(
            Intent(context, PipelineDetailActivity::class.java).putExtra(EXTRA_SITE_ID, siteId),
        )
    }

    fun openAlarm(context: Context, iotId: String? = null, siteType: Int = -1) {
        context.startActivity(
            Intent(context, PipelineAlarmActivity::class.java)
                .putExtra(EXTRA_IOT_ID, iotId)
                .putExtra(EXTRA_SITE_TYPE, siteType),
        )
    }
}
