package com.cq.iwa.readmeter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.R
import com.cq.iwa.databinding.ItemExtInfoBinding
import com.cq.iwa.feature.readmeter.ui.MeterExtInfoDisplay

class ExtInfoAdapter(
    private val onCall: (String) -> Unit,
    private val onEditPhone: (String) -> Unit,
    private val onEditDescribe: (String) -> Unit,
    private val onDebtDate: (String) -> Unit,
) : RecyclerView.Adapter<ExtInfoAdapter.Holder>() {

    private val items = mutableListOf<Pair<String, String>>()
    private var showPhoneDescribeActions = true

    fun submit(list: List<Pair<String, String>>, showPhoneDescribeActions: Boolean = true) {
        items.clear()
        items.addAll(list)
        this.showPhoneDescribeActions = showPhoneDescribeActions
        notifyDataSetChanged()
        val date = list.firstOrNull { it.first == MeterExtInfoDisplay.DEBT_LABEL }
            ?.second
            ?.let(MeterExtInfoDisplay::parseDebtDate)
        onDebtDate(date.orEmpty())
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemExtInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (label, value) = items[position]
        val ctx = holder.itemView.context
        val isDebt = label == MeterExtInfoDisplay.DEBT_LABEL
        val hasDebt = isDebt && MeterExtInfoDisplay.isArrears(value)
        holder.binding.tvLabel.text = label
        holder.binding.tvValue.text = MeterExtInfoDisplay.displayValue(label, value)
        holder.binding.tvValue.setTextColor(
            ContextCompat.getColor(ctx, if (hasDebt) R.color.crimson else R.color.navy),
        )
        holder.binding.tvValue.typeface = if (hasDebt) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val isPhone = label == "联系电话"
        val isRemark = label == "抄表员备注"
        val canCall = isPhone && value.isNotBlank()
        holder.binding.btnCall.visibility =
            if (showPhoneDescribeActions && canCall) View.VISIBLE else View.GONE
        holder.binding.btnEdit.visibility =
            if (showPhoneDescribeActions && (isPhone || isRemark)) View.VISIBLE else View.GONE
        holder.binding.btnCall.setOnClickListener { onCall(value) }
        holder.binding.btnEdit.setOnClickListener {
            if (isPhone) onEditPhone(value) else onEditDescribe(value)
        }
    }

    class Holder(val binding: ItemExtInfoBinding) : RecyclerView.ViewHolder(binding.root)
}
