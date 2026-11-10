package com.cq.iwa.dma

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.dma.ui.DmaPartitionUi
import com.cq.iwa.feature.dma.ui.encodeDmaPartition

object DmaNavigator {
    const val EXTRA_PARTITION = "partition"

    fun openList(context: Context) {
        context.startActivity(Intent(context, DmaListActivity::class.java))
    }

    fun openDetail(context: Context, item: DmaPartitionUi) {
        context.startActivity(
            Intent(context, DmaDetailActivity::class.java)
                .putExtra(EXTRA_PARTITION, encodeDmaPartition(item)),
        )
    }

    fun openCompare(context: Context) {
        context.startActivity(Intent(context, DmaCompareActivity::class.java))
    }
}
