package com.cq.iwa.dma

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.chart.MpLineCharts
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDmaCompareBinding
import com.cq.iwa.feature.dma.ui.DmaCompareMode
import com.cq.iwa.feature.dma.ui.DmaCompareUi
import com.cq.iwa.feature.dma.ui.DmaCompareViewModel
import com.cq.iwa.feature.dma.ui.DmaMetric
import com.cq.iwa.installation.InstDateRange
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DmaCompareActivity : IwaBaseActivity<ActivityDmaCompareBinding>() {

    private val viewModel: DmaCompareViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDmaCompareBinding =
        ActivityDmaCompareBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.tabCompare.setOnClickListener { viewModel.setMode(DmaCompareMode.COMPARE) }
        binding.tabYoy.setOnClickListener { viewModel.setMode(DmaCompareMode.YOY) }
        binding.tvLeft.setOnClickListener { pickPartition(left = true) }
        binding.tvRight.setOnClickListener { pickPartition(left = false) }
        buildMetrics()
        MpLineCharts.setup(binding.chart)
        MpLineCharts.bind(binding.chart, "", emptyList())
        val openBase = {
            val ui = viewModel.ui.value
            InstDateRange.pick(
                this,
                getString(R.string.dma_pick_range),
                InstDateRange.parseDay(ui.start),
                InstDateRange.parseDay(ui.end),
            ) { start, end ->
                val startText = InstDateRange.formatDay(start) ?: return@pick
                val endText = InstDateRange.formatDay(end) ?: return@pick
                viewModel.setRange(startText, endText)
            }
        }
        binding.baseRange.tvStartDate.setOnClickListener { openBase() }
        binding.baseRange.tvEndDate.setOnClickListener { openBase() }
        val openCompare = {
            val ui = viewModel.ui.value
            InstDateRange.pick(
                this,
                getString(R.string.dma_pick_range),
                InstDateRange.parseDay(ui.compareStart),
                InstDateRange.parseDay(ui.compareEnd),
            ) { start, end ->
                val startText = InstDateRange.formatDay(start) ?: return@pick
                val endText = InstDateRange.formatDay(end) ?: return@pick
                viewModel.setCompareRange(startText, endText)
            }
        }
        binding.compareRange.tvStartDate.setOnClickListener { openCompare() }
        binding.compareRange.tvEndDate.setOnClickListener { openCompare() }
        lifecycleScope.launch {
            viewModel.ui.collect { render(it) }
        }
        viewModel.load(DmaCompareMode.COMPARE)
    }

    private fun buildMetrics() {
        binding.metricRow.removeAllViews()
        DmaMetric.entries.forEach { metric ->
            val chip = TextView(this).apply {
                text = getString(metricLabel(metric))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(28, 12, 28, 12)
                setOnClickListener { viewModel.setMetric(metric) }
                tag = metric
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.marginEnd = 16
            binding.metricRow.addView(chip, params)
        }
    }

    private fun render(ui: DmaCompareUi) {
        val yoy = ui.mode == DmaCompareMode.YOY
        styleMode(binding.tabCompare, !yoy)
        styleMode(binding.tabYoy, yoy)
        binding.tvRight.isVisible = !yoy
        binding.tvCompareLabel.isVisible = yoy
        binding.compareRange.root.isVisible = yoy
        binding.tvLeft.text = ui.leftName.ifBlank { getString(R.string.dma_pick_partition) }
        binding.tvRight.text = ui.rightName.ifBlank { getString(R.string.dma_pick_partition) }
        InstDateRange.bind(
            binding.baseRange.tvStartDate,
            binding.baseRange.tvEndDate,
            InstDateRange.parseDay(ui.start),
            InstDateRange.parseDay(ui.end),
        )
        InstDateRange.bind(
            binding.compareRange.tvStartDate,
            binding.compareRange.tvEndDate,
            InstDateRange.parseDay(ui.compareStart),
            InstDateRange.parseDay(ui.compareEnd),
        )
        for (index in 0 until binding.metricRow.childCount) {
            val chip = binding.metricRow.getChildAt(index) as TextView
            val selected = chip.tag == ui.metric
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else R.drawable.bg_chip_unread)
            chip.setTextColor(ContextCompat.getColor(this, if (selected) R.color.primary else R.color.text_secondary))
        }
    }

    private fun styleMode(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_primary_button else R.drawable.bg_dma_pill)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_secondary))
    }

    private fun pickPartition(left: Boolean) {
        val names = viewModel.ui.value.partitions.map { it.name as CharSequence }
        if (names.isEmpty()) {
            showToast(getString(R.string.dma_no_partition))
            return
        }
        IwaDialogs.list(this, getString(R.string.dma_pick_partition), names) { index ->
            viewModel.selectPartition(left, names[index].toString())
        }
    }
}
