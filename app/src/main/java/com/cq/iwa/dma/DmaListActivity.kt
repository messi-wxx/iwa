package com.cq.iwa.dma

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDmaListBinding
import com.cq.iwa.databinding.ItemDmaMetricBinding
import com.cq.iwa.databinding.ItemDmaPartitionBinding
import com.cq.iwa.feature.dma.ui.DmaListUi
import com.cq.iwa.feature.dma.ui.DmaListViewModel
import com.cq.iwa.feature.dma.ui.DmaPartitionUi
import com.cq.iwa.feature.dma.ui.DmaSort
import com.cq.iwa.feature.dma.ui.DmaStatusFilter
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DmaListActivity : IwaBaseActivity<ActivityDmaListBinding>() {

    private val viewModel: DmaListViewModel by viewModels()
    private val adapter = PartitionAdapter { item ->
        DmaNavigator.openDetail(this, item)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDmaListBinding =
        ActivityDmaListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.fabCompare.setOnClickListener { DmaNavigator.openCompare(this) }
        binding.colTotal.setOnClickListener { viewModel.setStatus(DmaStatusFilter.ALL) }
        binding.colNormal.setOnClickListener { viewModel.setStatus(DmaStatusFilter.NORMAL) }
        binding.colWarn.setOnClickListener { viewModel.setStatus(DmaStatusFilter.WARN) }
        binding.colSevere.setOnClickListener { viewModel.setStatus(DmaStatusFilter.SEVERE) }
        binding.btnSearch.setOnClickListener { submitSearch() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }
        val arrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_down)?.mutate()
        val arrowSize = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp)
        arrow?.setBounds(0, 0, arrowSize, arrowSize)
        binding.btnSort.setCompoundDrawables(null, null, arrow, null)
        binding.btnSort.setOnClickListener { pickSort() }
        binding.btnFilter.setOnClickListener { DmaFilterSheet.show(this, viewModel) }
        binding.rvPartitions.layoutManager = LinearLayoutManager(this)
        binding.rvPartitions.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(
            enableLoadMore = true,
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
        )
        lifecycleScope.launch {
            viewModel.ui.collect { render(it) }
        }
        viewModel.refresh()
    }

    private fun render(ui: DmaListUi) {
        binding.tvTotal.text = ui.total.toString()
        binding.tvNormal.text = ui.normal.toString()
        binding.tvWarn.text = ui.warn.toString()
        binding.tvSevere.text = ui.severe.toString()
        binding.tvMeta.text = getString(R.string.dma_meta, ui.unknown)
        styleStat(binding.colTotal, ui.status == DmaStatusFilter.ALL)
        styleStat(binding.colNormal, ui.status == DmaStatusFilter.NORMAL)
        styleStat(binding.colWarn, ui.status == DmaStatusFilter.WARN)
        styleStat(binding.colSevere, ui.status == DmaStatusFilter.SEVERE)
        binding.btnSort.text = sortLabel(ui.sort, compact = true)
        val filterCount = listOfNotNull(ui.levelNum, ui.parentId).size
        binding.tvFilterBadge.isVisible = filterCount > 0
        binding.tvFilterBadge.text = filterCount.toString()
        binding.ivFilter.setColorFilter(
            ContextCompat.getColor(this, if (filterCount > 0) R.color.primary else R.color.navy),
        )
        renderTags(ui)
        adapter.submit(ui.partitions)
        binding.tvEmpty.isVisible = ui.loadTick > 0 && ui.partitions.isEmpty()
        binding.refreshLayout.finishSmart(ui.hasMore, enableLoadMore = true)
    }

    private fun submitSearch() {
        viewModel.setKeyword(binding.etSearch.text?.toString().orEmpty())
    }

    private fun styleStat(view: View, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else 0)
    }

    private fun pickSort() {
        val sorts = DmaSort.entries
        IwaDialogs.list(this, getString(R.string.dma_sort), sorts.map { sortLabel(it, compact = false) }) { index ->
            viewModel.setSort(sorts[index])
        }
    }

    private fun sortLabel(sort: DmaSort, compact: Boolean): String = when (sort) {
        DmaSort.STATUS -> if (compact) R.string.dma_sort_status_short else R.string.dma_sort_status
        DmaSort.KL -> if (compact) R.string.dma_sort_kl_short else R.string.dma_sort_kl
        DmaSort.EST_LEAK -> if (compact) R.string.dma_sort_leak_short else R.string.dma_sort_leak
        DmaSort.LEAK_RATE -> if (compact) R.string.dma_sort_leak_rate_short else R.string.dma_sort_leak_rate
        DmaSort.NRW -> if (compact) R.string.dma_sort_nrw_short else R.string.dma_sort_nrw
        DmaSort.FLOW -> if (compact) R.string.dma_sort_flow_short else R.string.dma_sort_flow
    }.let(::getString)

    private fun renderTags(ui: DmaListUi) {
        val box = binding.tagBox
        box.removeAllViews()
        val tags = buildList {
            if (ui.levelName.isNotBlank()) {
                add(getString(R.string.dma_filter_level_tag, ui.levelName) to {
                    viewModel.setScope(null, "", ui.parentId, ui.parentName)
                })
            }
            if (ui.parentName.isNotBlank()) {
                add(getString(R.string.dma_filter_parent_tag, ui.parentName) to {
                    viewModel.setScope(ui.levelNum, ui.levelName, null, "")
                })
            }
        }
        binding.tagScroll.isVisible = tags.isNotEmpty()
        tags.forEach { (text, clear) -> box.addView(tagView(text, muted = false, clear)) }
        if (tags.isNotEmpty()) {
            box.addView(tagView(getString(R.string.dma_filter_clear), muted = true) {
                viewModel.setScope(null, "", null, "")
            })
        }
    }

    private fun tagView(text: String, muted: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@DmaListActivity, if (muted) R.color.text_secondary else R.color.primary))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(com.intuit.ssp.R.dimen._11ssp))
            setBackgroundResource(if (muted) R.drawable.bg_input_meter else R.drawable.bg_soft_button)
            val padH = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
            val padV = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
            setPadding(padH, padV, padH, padV)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
            }
        }
    }
}

private class PartitionAdapter(
    private val onClick: (DmaPartitionUi) -> Unit,
) : RecyclerView.Adapter<PartitionAdapter.Holder>() {

    private var items: List<DmaPartitionUi> = emptyList()

    fun submit(list: List<DmaPartitionUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDmaPartitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val binding = holder.binding
        binding.tvName.text = item.name
        binding.tvLevel.text = item.levelName
        binding.tvLevel.isVisible = item.levelName.isNotBlank()
        val code = item.code.takeIf { it.isNotBlank() }?.let { "# $it" }.orEmpty()
        binding.tvMeta.text = listOf(code, item.parentName, item.analyzeDate)
            .filter { it.isNotBlank() }
            .joinToString("    ")
        val (statusText, statusColor, statusBg) = when (item.status) {
            0 -> Triple(R.string.dma_normal, R.color.success, R.color.dma_status_normal_bg)
            1 -> Triple(R.string.dma_warn, R.color.warning, R.color.dma_status_warn_bg)
            2 -> Triple(R.string.dma_severe, R.color.crimson, R.color.dma_status_severe_bg)
            else -> Triple(R.string.dma_unknown, R.color.text_secondary, R.color.dma_tag)
        }
        binding.tvStatus.setText(statusText)
        binding.tvStatus.setTextColor(ContextCompat.getColor(context, statusColor))
        ViewCompat.setBackgroundTintList(
            binding.statusBadge,
            ColorStateList.valueOf(ContextCompat.getColor(context, statusBg)),
        )
        ViewCompat.setBackgroundTintList(
            binding.vStatusDot,
            ColorStateList.valueOf(ContextCompat.getColor(context, statusColor)),
        )
        val changeColor = if (item.supplyChangeUp == true) R.color.crimson else R.color.success
        val change = if (item.supplyChange.isBlank()) {
            ""
        } else {
            val arrow = if (item.supplyChangeUp == true) "▲" else "▼"
            "$arrow ${context.getString(R.string.dma_vs_yesterday, item.supplyChange)}"
        }
        bindCell(
            binding.cellSupply,
            context.getString(R.string.dma_card_supply),
            item.supply,
            context.getString(R.string.dma_unit_cubic),
            change,
            R.color.dma_value_blue,
            changeColor,
        )
        bindCell(
            binding.cellNight,
            context.getString(R.string.dma_card_night),
            item.nightFlow,
            context.getString(R.string.dma_unit_cubic),
            item.nightWindow.takeIf { it.isNotBlank() }?.let {
                context.getString(R.string.dma_night_window, it)
            }.orEmpty(),
            R.color.dma_value_purple,
        )
        bindCell(
            binding.cellMinNight,
            context.getString(R.string.dma_card_min_night),
            item.minNightFlow,
            context.getString(R.string.dma_unit_cubic_hour),
            item.minNightAt.takeIf { it.isNotBlank() }?.let {
                context.getString(R.string.dma_appear_at, it)
            }.orEmpty(),
            R.color.navy,
        )
        bindCell(
            binding.cellNightRatio,
            context.getString(R.string.dma_card_night_ratio),
            item.nightRatio,
            context.getString(R.string.dma_unit_percent),
            context.getString(R.string.dma_night_ratio_hint),
            ratioColor(item.nightRatioValue),
        )
        bindCell(
            binding.cellKl,
            context.getString(R.string.dma_card_kl),
            item.kl,
            context.getString(R.string.dma_unit_percent),
            context.getString(R.string.dma_kl_hint),
            ratioColor(item.klValue),
        )
        val estHint = when (item.estLeakOver) {
            true -> context.getString(R.string.dma_est_over)
            false -> context.getString(R.string.dma_est_under)
            null -> ""
        }
        bindCell(
            binding.cellEst,
            context.getString(R.string.dma_card_est),
            item.estLeak,
            context.getString(R.string.dma_unit_cubic),
            estHint,
            when (item.estLeakOver) {
                true -> R.color.warning
                false -> R.color.success
                null -> R.color.navy
            },
        )
        bindCell(
            binding.cellLeak,
            context.getString(R.string.dma_card_month_leak),
            item.leakRate,
            context.getString(R.string.dma_unit_percent),
            item.leakMonth,
            leakColor(item.leakRateValue),
        )
        bindCell(
            binding.cellNrw,
            context.getString(R.string.dma_card_nrw),
            item.nrw,
            context.getString(R.string.dma_unit_percent),
            item.leakMonth,
            leakColor(item.nrwValue),
        )
        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun bindCell(
        cell: ItemDmaMetricBinding,
        label: String,
        value: String,
        unit: String,
        sub: String,
        valueColor: Int,
        subColor: Int = R.color.text_hint,
    ) {
        val context = cell.root.context
        cell.tvLabel.text = label
        cell.tvValue.text = value.ifBlank { "—" }
        cell.tvUnit.text = unit
        cell.tvUnit.isVisible = value.isNotBlank()
        cell.tvSub.text = sub
        cell.tvSub.isVisible = sub.isNotBlank()
        cell.tvValue.setTextColor(ContextCompat.getColor(context, valueColor))
        cell.tvSub.setTextColor(ContextCompat.getColor(context, subColor))
    }

    private fun ratioColor(value: Double?): Int = when {
        value == null -> R.color.navy
        value > 30 -> R.color.crimson
        value >= 20 -> R.color.dma_value_yellow
        else -> R.color.success
    }

    private fun leakColor(value: Double?): Int = when {
        value == null -> R.color.navy
        value > 15 -> R.color.crimson
        value > 12 -> R.color.warning
        value >= 9 -> R.color.dma_value_yellow
        else -> R.color.success
    }

    class Holder(val binding: ItemDmaPartitionBinding) : RecyclerView.ViewHolder(binding.root)
}
