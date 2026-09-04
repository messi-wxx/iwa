package com.cq.iwa.pipeline

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityPipelineFollowBinding
import com.cq.iwa.databinding.ItemPipelineMetricBinding
import com.cq.iwa.databinding.ItemPipelineSiteBinding
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import com.cq.iwa.feature.pipeline.network.PipelineFollowDeviceDto
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.ui.PipelineFollowViewModel
import com.cq.iwa.installation.setupInstHeader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PipelineFollowActivity : IwaBaseActivity<ActivityPipelineFollowBinding>() {

    private val viewModel: PipelineFollowViewModel by viewModels()
    private lateinit var adapter: SiteAdapter

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityPipelineFollowBinding =
        ActivityPipelineFollowBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.pipeline_follow_title),
            R.menu.menu_pipeline_follow,
        ) { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    PipelineNavigator.openTree(this)
                    true
                }
                R.id.action_records -> {
                    PipelineNavigator.openAlarm(this)
                    true
                }
                else -> false
            }
        }
        adapter = SiteAdapter(
            onSite = { PipelineNavigator.openDetail(this, it.siteId) },
            onUnfollow = { viewModel.toggleMetric(it.siteId, null) },
            onFollow = { site, metric -> viewModel.toggleMetric(site.siteId, metric) },
            onMetric = { metric ->
                PipelineChartSheet.show(
                    activity = this,
                    title = PipelineFormat.displayName(metric.name, metric.unit).ifBlank { metric.metricId },
                    start = PipelineFormat.beforeDays(1),
                    end = PipelineFormat.now("yyyy-MM-dd"),
                    onQuery = { start, end -> viewModel.loadChart(metric, start, end) },
                    chartFlow = viewModel.chart,
                )
            },
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        ItemTouchHelper(DragCallback(adapter) { viewModel.updateOrder(it) })
            .attachToRecyclerView(binding.rvItems)
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                adapter.submit(ui.items)
                binding.tvEmpty.isVisible = ui.empty
                binding.rvItems.isVisible = !ui.empty
            }
        }
        lifecycleScope.launch {
            if (Build.VERSION.SDK_INT >= 33) {
                permissionRequester.request(Manifest.permission.POST_NOTIFICATIONS)
            }
            PipelineAlarmService.start(this@PipelineFollowActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFollows()
    }

    override fun onDestroy() {
        viewModel.disconnect()
        super.onDestroy()
    }

    private class SiteAdapter(
        private val onSite: (PipelineFollowDeviceDto) -> Unit,
        private val onUnfollow: (PipelineFollowDeviceDto) -> Unit,
        private val onFollow: (PipelineFollowDeviceDto, PipelineMetricDto) -> Unit,
        private val onMetric: (PipelineMetricDto) -> Unit,
    ) : RecyclerView.Adapter<SiteAdapter.Holder>() {
        private var items: MutableList<PipelineFollowDeviceDto> = mutableListOf()

        fun submit(value: List<PipelineFollowDeviceDto>) {
            items = value.toMutableList()
            notifyDataSetChanged()
        }

        fun items(): List<PipelineFollowDeviceDto> = items

        fun move(from: Int, to: Int) {
            if (from !in items.indices || to !in items.indices) return
            val item = items.removeAt(from)
            items.add(to, item)
            notifyItemMoved(from, to)
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPipelineSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvName.text = item.siteName
            holder.binding.tvAddress.text = item.address
            holder.binding.tvAddress.isVisible = !item.address.isNullOrBlank()
            holder.binding.tvUnfollow.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.cancel_site_follow_icon, 0, 0, 0,
            )
            holder.binding.tvUnfollow.setOnClickListener { onUnfollow(item) }
            holder.itemView.setOnClickListener { onSite(item) }
            val metricAdapter = MetricAdapter(
                onFollow = { onFollow(item, it) },
                onMetric = onMetric,
            )
            holder.binding.rvMetrics.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.binding.rvMetrics.adapter = metricAdapter
            metricAdapter.submit(item.siteMetrics)
        }

        class Holder(val binding: ItemPipelineSiteBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private class MetricAdapter(
        private val onFollow: (PipelineMetricDto) -> Unit,
        private val onMetric: (PipelineMetricDto) -> Unit,
    ) : RecyclerView.Adapter<MetricAdapter.Holder>() {
        private var items: List<PipelineMetricDto> = emptyList()
        fun submit(value: List<PipelineMetricDto>) {
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPipelineMetricBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvName.text = PipelineFormat.displayName(item.name, item.unit)
            holder.binding.tvDate.text = PipelineFormat.fromTimestamp(item.timestamp)
            holder.binding.tvValue.text = PipelineFormat.formatFloat(item.value, item.digit)
            holder.binding.ivFollow.setImageResource(if (item.follow) R.drawable.follow else R.drawable.follow_cancel)
            holder.binding.ivFollow.setOnClickListener { onFollow(item) }
            holder.binding.content.setOnClickListener { onMetric(item) }
        }

        class Holder(val binding: ItemPipelineMetricBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private class DragCallback(
        private val adapter: SiteAdapter,
        private val onDone: (List<PipelineFollowDeviceDto>) -> Unit,
    ) : ItemTouchHelper.Callback() {
        private var dragging = false
        private var from = -1
        private var to = -1

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            to = target.adapterPosition
            adapter.move(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null && !dragging) {
                dragging = true
                from = viewHolder.adapterPosition
                to = from
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && dragging) {
                if (from != to) onDone(adapter.items())
                dragging = false
            }
        }

        override fun isLongPressDragEnabled() = true
        override fun isItemViewSwipeEnabled() = false
    }
}
