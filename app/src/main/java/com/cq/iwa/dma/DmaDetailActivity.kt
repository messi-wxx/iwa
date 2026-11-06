package com.cq.iwa.dma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.chart.MpLineCharts
import com.cq.iwa.databinding.ActivityDmaDetailBinding
import com.cq.iwa.databinding.ItemDmaMetricBinding
import com.cq.iwa.feature.dma.ui.DmaDetailTab
import com.cq.iwa.feature.dma.ui.DmaDetailUi
import com.cq.iwa.feature.dma.ui.DmaDetailViewModel
import com.cq.iwa.feature.dma.ui.DmaMetricCell
import com.cq.iwa.installation.InstDateRange
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DmaDetailActivity : IwaBaseActivity<ActivityDmaDetailBinding>() {

    private val viewModel: DmaDetailViewModel by viewModels()
    private val metricAdapter = MetricAdapter()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDmaDetailBinding =
        ActivityDmaDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.rvMetrics.layoutManager = GridLayoutManager(this, 2)
        binding.rvMetrics.adapter = metricAdapter
        binding.tabTrend.setOnClickListener { viewModel.setTab(DmaDetailTab.TREND) }
        binding.tabDaily.setOnClickListener { viewModel.setTab(DmaDetailTab.DAILY) }
        binding.tabMonth.setOnClickListener { viewModel.setTab(DmaDetailTab.MONTH) }
        binding.tabFlow.setOnClickListener { viewModel.setTab(DmaDetailTab.FLOW) }
        binding.btnLoadMore.setOnClickListener { viewModel.loadMoreHistory() }
        MpLineCharts.setup(binding.chartTrend)
        MpLineCharts.bind(binding.chartTrend, "", emptyList())
        bindRange(binding.trendRange.tvStartDate, binding.trendRange.tvEndDate, { ui -> ui.trendStart to ui.trendEnd }) { start, end ->
            viewModel.setTrendRange(start, end)
        }
        bindRange(binding.historyRange.tvStartDate, binding.historyRange.tvEndDate, { ui -> ui.historyStart to ui.historyEnd }) { start, end ->
            viewModel.setHistoryRange(start, end)
        }
        bindRange(binding.monthRange.tvStartDate, binding.monthRange.tvEndDate, { ui -> ui.monthStart to ui.monthEnd }) { start, end ->
            viewModel.setMonthRange(start, end)
        }
        binding.tvFlowDate.setOnClickListener {
            DmaDates.pickDay(this, viewModel.ui.value.flowDate) { viewModel.setFlowDate(it) }
        }
        lifecycleScope.launch {
            viewModel.ui.collect { render(it) }
        }
        viewModel.load(
            intent.getStringExtra(DmaNavigator.EXTRA_ID).orEmpty(),
            intent.getStringExtra(DmaNavigator.EXTRA_NAME).orEmpty(),
        )
    }

    private fun bindRange(
        startView: TextView,
        endView: TextView,
        current: (DmaDetailUi) -> Pair<String, String>,
        onPicked: (String, String) -> Unit,
    ) {
        val open = {
            val (start, end) = current(viewModel.ui.value)
            InstDateRange.pick(
                this,
                getString(R.string.dma_pick_range),
                InstDateRange.parseDay(start),
                InstDateRange.parseDay(end),
            ) { startMillis, endMillis ->
                val startText = InstDateRange.formatDay(startMillis) ?: return@pick
                val endText = InstDateRange.formatDay(endMillis) ?: return@pick
                onPicked(startText, endText)
            }
        }
        startView.setOnClickListener { open() }
        endView.setOnClickListener { open() }
    }

    private fun render(ui: DmaDetailUi) {
        binding.tvTitle.text = ui.name.ifBlank { getString(R.string.dma_detail_title) }
        binding.tvInfo.text = getString(
            R.string.dma_meta,
            ui.dataDate.ifBlank { "—" },
            ui.nightWindow.ifBlank { "—" },
            ui.updatedAt.ifBlank { "—" },
        )
        metricAdapter.submit(ui.metrics)
        styleTab(binding.tabTrend, ui.tab == DmaDetailTab.TREND)
        styleTab(binding.tabDaily, ui.tab == DmaDetailTab.DAILY)
        styleTab(binding.tabMonth, ui.tab == DmaDetailTab.MONTH)
        styleTab(binding.tabFlow, ui.tab == DmaDetailTab.FLOW)
        binding.panelTrend.isVisible = ui.tab == DmaDetailTab.TREND
        binding.panelDaily.isVisible = ui.tab == DmaDetailTab.DAILY
        binding.panelMonth.isVisible = ui.tab == DmaDetailTab.MONTH
        binding.panelFlow.isVisible = ui.tab == DmaDetailTab.FLOW
        InstDateRange.bind(binding.trendRange.tvStartDate, binding.trendRange.tvEndDate, InstDateRange.parseDay(ui.trendStart), InstDateRange.parseDay(ui.trendEnd))
        InstDateRange.bind(binding.historyRange.tvStartDate, binding.historyRange.tvEndDate, InstDateRange.parseDay(ui.historyStart), InstDateRange.parseDay(ui.historyEnd))
        InstDateRange.bind(binding.monthRange.tvStartDate, binding.monthRange.tvEndDate, InstDateRange.parseDay(ui.monthStart), InstDateRange.parseDay(ui.monthEnd))
        binding.tvFlowDate.text = ui.flowDate.ifBlank { getString(R.string.dma_pick_day) }
        binding.tvHistoryEmpty.isVisible = ui.history.isEmpty()
        binding.tvTrendEmpty.isVisible = true
    }

    private fun styleTab(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else R.drawable.bg_chip_unread)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.primary else R.color.text_secondary))
    }
}

private class MetricAdapter : RecyclerView.Adapter<MetricAdapter.Holder>() {
    private var items: List<DmaMetricCell> = emptyList()

    fun submit(list: List<DmaMetricCell>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDmaMetricBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = holder.itemView.context.getString(metricLabel(item.metric))
        holder.binding.tvValue.text = item.value
    }

    class Holder(val binding: ItemDmaMetricBinding) : RecyclerView.ViewHolder(binding.root)
}
