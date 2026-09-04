package com.cq.iwa.pipeline

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityPipelineAlarmBinding
import com.cq.iwa.databinding.ItemPipelineAlarmBinding
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import com.cq.iwa.feature.pipeline.network.PipelineAlarmRecordDto
import com.cq.iwa.feature.pipeline.ui.PipelineAlarmViewModel
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PipelineAlarmActivity : IwaBaseActivity<ActivityPipelineAlarmBinding>() {

    private val viewModel: PipelineAlarmViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background
    override fun inflateBinding() = ActivityPipelineAlarmBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        val adapter = AlarmAdapter()
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(
            enableLoadMore = true,
            onRefresh = { viewModel.refresh(overlay = false) },
            onLoadMore = { viewModel.loadMore() },
        )
        binding.dateRange.setOnClickListener { showDatePicker() }
        binding.chipActive.setOnCheckedChangeListener { _, checked -> viewModel.toggleStatus(1, checked) }
        binding.chipCleared.setOnCheckedChangeListener { _, checked -> viewModel.toggleStatus(2, checked) }
        binding.chipAck.setOnCheckedChangeListener { _, checked -> viewModel.toggleStatus(3, checked) }
        binding.chipUnack.setOnCheckedChangeListener { _, checked -> viewModel.toggleStatus(4, checked) }
        lifecycleScope.launch {
            var lastVersion = -1
            viewModel.ui.collect { ui ->
                binding.tvStart.text = ui.startDate
                binding.tvEnd.text = ui.endDate
                adapter.submit(ui.items)
                binding.tvEmpty.isVisible = ui.empty
                binding.rvItems.isVisible = !ui.empty
                if (ui.version != lastVersion) {
                    lastVersion = ui.version
                    binding.refreshLayout.finishSmart(ui.hasMore, enableLoadMore = true)
                }
            }
        }
        viewModel.setup(
            iotId = intent.getStringExtra(PipelineNavigator.EXTRA_IOT_ID),
            siteType = intent.getIntExtra(PipelineNavigator.EXTRA_SITE_TYPE, -1),
            start = PipelineFormat.now("yyyy-MM") + "-01",
            end = PipelineFormat.now("yyyy-MM-dd"),
        )
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.pipeline_alarm))
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = range.first?.let { fmt.format(Date(it)) } ?: return@addOnPositiveButtonClickListener
            val end = range.second?.let { fmt.format(Date(it)) } ?: return@addOnPositiveButtonClickListener
            viewModel.setDates(start, end)
        }
        picker.show(supportFragmentManager, "pipeline_alarm_range")
    }

    private class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.Holder>() {
        private var items: List<PipelineAlarmRecordDto> = emptyList()
        fun submit(value: List<PipelineAlarmRecordDto>) {
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPipelineAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvName.text = item.name
            holder.binding.tvStatus.text = item.unionStatuName
            holder.binding.tvStatus.background = ContextCompat.getDrawable(
                holder.itemView.context,
                when (item.unionStatuName) {
                    "激活未确认" -> R.drawable.bg_urgepayment_fee_qty
                    "激活已确认" -> R.drawable.bg_status_active
                    "清除未确认" -> R.drawable.bg_status_cleared_
                    else -> R.drawable.bg_status_cleared
                },
            )
            holder.binding.tvOriginator.text = item.originatorName
            holder.binding.tvSeverity.text = item.serverityName
            holder.binding.tvSeverity.setTextColor(
                when (item.serverityName) {
                    "危险" -> Color.parseColor("#E74C3C")
                    "重要" -> Color.parseColor("#F39C12")
                    "次要" -> Color.parseColor("#F1C40F")
                    "警告" -> Color.parseColor("#3498DB")
                    else -> Color.parseColor("#95A5A6")
                },
            )
            holder.binding.tvDuration.isVisible = item.durationTime.isNotEmpty()
            holder.binding.tvDuration.text = item.durationTime
            holder.binding.tvDetails.text = item.details
            holder.binding.tvTime.text = PipelineFormat.fromIso(item.createdTime)
        }

        class Holder(val binding: ItemPipelineAlarmBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
