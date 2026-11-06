package com.cq.iwa.dma

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDmaListBinding
import com.cq.iwa.databinding.ItemDmaPartitionBinding
import com.cq.iwa.feature.dma.ui.DmaListUi
import com.cq.iwa.feature.dma.ui.DmaListViewModel
import com.cq.iwa.feature.dma.ui.DmaCompareMode
import com.cq.iwa.feature.dma.ui.DmaPartitionUi
import com.cq.iwa.feature.dma.ui.DmaStatusFilter
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DmaListActivity : IwaBaseActivity<ActivityDmaListBinding>() {

    private val viewModel: DmaListViewModel by viewModels()
    private val adapter = PartitionAdapter { item ->
        DmaNavigator.openDetail(this, item.id, item.name)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDmaListBinding =
        ActivityDmaListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { refresh() }
        binding.btnCompare.setOnClickListener { DmaNavigator.openCompare(this, DmaCompareMode.COMPARE) }
        binding.btnYoy.setOnClickListener { DmaNavigator.openCompare(this, DmaCompareMode.YOY) }
        binding.chipAll.setOnClickListener { viewModel.setStatus(DmaStatusFilter.ALL) }
        binding.chipNormal.setOnClickListener { viewModel.setStatus(DmaStatusFilter.NORMAL) }
        binding.chipWarn.setOnClickListener { viewModel.setStatus(DmaStatusFilter.WARN) }
        binding.chipSevere.setOnClickListener { viewModel.setStatus(DmaStatusFilter.SEVERE) }
        binding.tvSort.setOnClickListener { pickSort() }
        binding.tvLevel.setOnClickListener { pickSingle(getString(R.string.dma_level), listOf(getString(R.string.dma_all_levels)), viewModel::setLevel) }
        binding.tvParent.setOnClickListener { pickSingle(getString(R.string.dma_parent), listOf(getString(R.string.dma_all_parents)), viewModel::setParent) }
        binding.etSearch.doAfterTextChanged { viewModel.setKeyword(it?.toString().orEmpty()) }
        binding.rvPartitions.layoutManager = LinearLayoutManager(this)
        binding.rvPartitions.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(onRefresh = { refresh() })
        lifecycleScope.launch {
            viewModel.ui.collect { render(it) }
        }
    }

    private fun refresh() {
        viewModel.refresh()
        binding.refreshLayout.finishSmart()
    }

    private fun render(ui: DmaListUi) {
        binding.tvTotal.text = ui.total.toString()
        binding.tvNormal.text = ui.normal.toString()
        binding.tvWarn.text = ui.warn.toString()
        binding.tvSevere.text = ui.severe.toString()
        binding.tvMeta.text = getString(
            R.string.dma_meta,
            ui.dataDate.ifBlank { "—" },
            ui.nightWindow.ifBlank { "—" },
            ui.updatedAt.ifBlank { "—" },
        )
        binding.tvSort.text = ui.sortName
        binding.tvLevel.text = ui.levelName
        binding.tvParent.text = ui.parentName
        styleChip(binding.chipAll, ui.status == DmaStatusFilter.ALL)
        styleChip(binding.chipNormal, ui.status == DmaStatusFilter.NORMAL)
        styleChip(binding.chipWarn, ui.status == DmaStatusFilter.WARN)
        styleChip(binding.chipSevere, ui.status == DmaStatusFilter.SEVERE)
        adapter.submit(ui.partitions)
        binding.tvEmpty.isVisible = ui.partitions.isEmpty()
        binding.refreshLayout.finishSmart()
    }

    private fun styleChip(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else R.drawable.bg_chip_unread)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.primary else R.color.text_secondary))
    }

    private fun pickSort() {
        val options = listOf(
            getString(R.string.dma_sort_default),
            getString(R.string.dma_metric_supply),
            getString(R.string.dma_metric_kl),
            getString(R.string.dma_metric_leak),
            getString(R.string.dma_metric_night_ratio),
        )
        IwaDialogs.list(this, getString(R.string.dma_sort), options) { index ->
            viewModel.setSort(options[index].toString())
        }
    }

    private fun pickSingle(title: String, options: List<String>, onPick: (String) -> Unit) {
        IwaDialogs.list(this, title, options.map { it as CharSequence }) { index ->
            onPick(options[index])
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
        holder.binding.tvName.text = item.name
        holder.binding.tvParent.text = listOf(item.levelName, item.parentName).filter { it.isNotBlank() }.joinToString(" · ")
        val (text, color) = when (item.status) {
            2 -> context.getString(R.string.dma_severe) to R.color.crimson
            1 -> context.getString(R.string.dma_warn) to R.color.warning
            else -> context.getString(R.string.dma_normal) to R.color.success
        }
        holder.binding.tvStatus.text = text
        holder.binding.tvStatus.setTextColor(ContextCompat.getColor(context, color))
        holder.binding.tvMetrics.text = context.getString(
            R.string.dma_card_metrics,
            item.supply.ifBlank { "—" },
            item.kl.ifBlank { "—" },
            item.leakRate.ifBlank { "—" },
            item.nightRatio.ifBlank { "—" },
        )
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemDmaPartitionBinding) : RecyclerView.ViewHolder(binding.root)
}
