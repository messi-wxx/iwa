package com.cq.iwa.dma

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.dma.ui.DmaCompareMode

object DmaNavigator {
    const val EXTRA_ID = "partitionId"
    const val EXTRA_NAME = "partitionName"
    const val EXTRA_MODE = "mode"

    fun openList(context: Context) {
        context.startActivity(Intent(context, DmaListActivity::class.java))
    }

    fun openDetail(context: Context, id: String, name: String) {
        context.startActivity(
            Intent(context, DmaDetailActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_NAME, name),
        )
    }

    fun openCompare(context: Context, mode: DmaCompareMode) {
        context.startActivity(
            Intent(context, DmaCompareActivity::class.java)
                .putExtra(EXTRA_MODE, mode.name),
        )
    }
}
