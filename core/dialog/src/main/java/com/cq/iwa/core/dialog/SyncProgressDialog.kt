package com.cq.iwa.core.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import androidx.core.view.isVisible

class SyncProgressDialog(context: Context) {

    private val dialog = Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_sync_progress, null))
    }

    private val tvTitle = dialog.findViewById<TextView>(R.id.tvSyncTitle)
    private val bookRow = dialog.findViewById<android.view.View>(R.id.bookRow)
    private val tvTaskName = dialog.findViewById<TextView>(R.id.tvTaskName)
    private val tvTotalProgress = dialog.findViewById<TextView>(R.id.tvTotalProgress)
    private val tvTip = dialog.findViewById<TextView>(R.id.tvTip)
    private val tvPercent = dialog.findViewById<TextView>(R.id.tvPercent)

    init {
        tvTip.isSelected = true
    }

    val isShowing: Boolean get() = dialog.isShowing

    fun show() {
        if (dialog.isShowing) return
        runCatching {
            dialog.show()
            dialog.applyCardWindow(0.88f)
        }
    }

    fun update(
        title: String = "同步数据中",
        taskName: String = "",
        totalProgress: String = "",
        tip: String = "",
        percent: String = "",
    ) {
        tvTitle.text = title.ifBlank { "同步数据中" }
        val showBook = taskName.isNotBlank() || totalProgress.isNotBlank()
        bookRow.isVisible = showBook
        if (taskName.isNotBlank()) tvTaskName.text = taskName
        if (totalProgress.isNotBlank()) tvTotalProgress.text = totalProgress
        if (tip.isNotBlank()) {
            tvTip.text = tip
            tvTip.isSelected = true
        }
        tvPercent.text = percent
        if (!dialog.isShowing) show()
    }

    fun dismiss() {
        runCatching {
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}
