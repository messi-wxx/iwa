package com.cq.iwa.urgepayment

import android.content.Context
import android.content.Intent

object UrgePaymentNavigator {

    const val EXTRA_BOOK_ID = "bookId"
    const val EXTRA_TASK_NAME = "taskName"
    const val EXTRA_CLIENT_CODE = "clientCode"

    fun open(context: Context) {
        context.startActivity(Intent(context, UrgeTaskListActivity::class.java))
    }

    fun openMeterList(context: Context, bookId: Int, taskName: String) {
        context.startActivity(
            Intent(context, UrgeMeterListActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_TASK_NAME, taskName),
        )
    }

    fun openDetail(context: Context, clientCode: String) {
        context.startActivity(
            Intent(context, UrgePaymentDetailActivity::class.java)
                .putExtra(EXTRA_CLIENT_CODE, clientCode),
        )
    }
}
