package com.cq.iwa.urgepayment

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.R
import com.cq.iwa.databinding.DialogUrgeHistoryBinding
import com.cq.iwa.databinding.DialogUrgeSearchBinding
import com.cq.iwa.databinding.ItemUrgeHistoryBinding
import com.cq.iwa.databinding.ItemUrgeSearchBinding
import com.cq.iwa.feature.urgepayment.ui.UrgeReadHistoryUi
import com.cq.iwa.feature.urgepayment.ui.UrgeSearchItemUi

internal fun showUrgeSearchDialog(
    context: Context,
    items: List<UrgeSearchItemUi>,
    onSelect: (UrgeSearchItemUi) -> Unit,
): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val binding = DialogUrgeSearchBinding.inflate(LayoutInflater.from(context))
    dialog.setContentView(binding.root)
    dialog.setCancelable(true)
    binding.tvDialogTitle.text = context.getString(R.string.urge_search_result)
    val maxHeight = (context.resources.displayMetrics.heightPixels * 0.55f).toInt()
    binding.rvDialogList.layoutParams = binding.rvDialogList.layoutParams.apply { height = maxHeight }
    binding.rvDialogList.layoutManager = LinearLayoutManager(context)
    binding.rvDialogList.adapter = UrgeSearchAdapter(items) { item ->
        dialog.dismiss()
        onSelect(item)
    }
    dialog.show()
    dialog.applyUrgeCardWindow()
    return dialog
}

internal fun showUrgeHistoryDialog(
    context: Context,
    items: List<UrgeReadHistoryUi>,
): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val binding = DialogUrgeHistoryBinding.inflate(LayoutInflater.from(context))
    dialog.setContentView(binding.root)
    dialog.setCancelable(true)
    val maxHeight = (context.resources.displayMetrics.heightPixels * 0.6f).toInt()
    binding.rvHistory.layoutParams = binding.rvHistory.layoutParams.apply { height = maxHeight }
    binding.rvHistory.layoutManager = LinearLayoutManager(context)
    binding.rvHistory.adapter = UrgeHistoryAdapter(items)
    dialog.show()
    dialog.applyUrgeCardWindow()
    return dialog
}

private fun Dialog.applyUrgeCardWindow(widthRatio: Float = 0.86f) {
    window?.let { window ->
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0.45f)
        val width = (context.resources.displayMetrics.widthPixels * widthRatio).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }
}

private class UrgeSearchAdapter(
    private val items: List<UrgeSearchItemUi>,
    private val onClick: (UrgeSearchItemUi) -> Unit,
) : RecyclerView.Adapter<UrgeSearchAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvAddress.text = item.address
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemUrgeSearchBinding) : RecyclerView.ViewHolder(binding.root)
}

private class UrgeHistoryAdapter(
    private val items: List<UrgeReadHistoryUi>,
) : RecyclerView.Adapter<UrgeHistoryAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.binding.tvMeter.text = "${ctx.getString(R.string.urge_history_meter)}  ${item.meterCode}"
        holder.binding.tvDate.text = "${ctx.getString(R.string.urge_history_date)}  ${item.readDate}"
        holder.binding.tvReading.text = "${ctx.getString(R.string.urge_history_reading)}  ${item.reading}"
        holder.binding.tvSource.text = "${ctx.getString(R.string.urge_history_source)}  ${item.source}"
        holder.binding.tvReader.text = "${ctx.getString(R.string.urge_history_reader)}  ${item.readUser}"
        holder.binding.tvAudit.text = "${ctx.getString(R.string.urge_history_audit)}  ${item.auditUser}"
    }

    class Holder(val binding: ItemUrgeHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
