package com.cq.iwa.dma

import android.content.res.ColorStateList
import android.os.Bundle
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.chart.MpLineCharts
import com.cq.iwa.databinding.ActivityDmaDetailBinding
import com.cq.iwa.databinding.ItemDmaFlowDeviceBinding
import com.cq.iwa.databinding.ItemDmaHistoryBinding
import com.cq.iwa.databinding.ItemDmaHistoryCellBinding
import com.cq.iwa.databinding.ItemDmaTileBinding
import com.cq.iwa.feature.dma.ui.DmaDetailUi
import com.cq.iwa.feature.dma.ui.DmaDetailViewModel
import com.cq.iwa.feature.dma.ui.DmaHistoryKind
import com.cq.iwa.feature.dma.ui.DmaHistoryRowUi
import com.cq.iwa.feature.dma.ui.DmaTileUi
import com.cq.iwa.feature.dma.ui.DmaTrendTab
import com.cq.iwa.feature.dma.ui.DmaValueTone
import com.cq.iwa.feature.dma.ui.decodeDmaPartition
import com.cq.iwa.installation.InstDateRange
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DmaDetailActivity : IwaBaseActivity<ActivityDmaDetailBinding>() {

    private val viewModel: DmaDetailViewModel by viewModels()
    private val tileAdapter = TileAdapter { viewModel.setTrend(it.trend) }
    private val historyAdapter = HistoryAdapter { index ->
        val device = viewModel.ui.value.devices.getOrNull(index) ?: return@HistoryAdapter
        DmaDeviceSheet.show(this, device, viewModel.ui.value.flowDate)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDmaDetailBinding =
        ActivityDmaDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.rvTiles.layoutManager = GridLayoutManager(this, 4)
        binding.rvTiles.adapter = tileAdapter
        binding.rvTiles.addItemDecoration(GridLineDecoration(resources.displayMetrics.density))
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
        binding.btnTrendPrev.setOnClickListener { viewModel.stepTrend(-1) }
        binding.btnTrendNext.setOnClickListener { viewModel.stepTrend(1) }
        binding.tabDay.setOnClickListener { viewModel.setTrend(DmaTrendTab.DAY) }
        binding.tabNight.setOnClickListener { viewModel.setTrend(DmaTrendTab.NIGHT) }
        binding.tabMin.setOnClickListener { viewModel.setTrend(DmaTrendTab.MIN_NIGHT) }
        binding.tabRatio.setOnClickListener { viewModel.setTrend(DmaTrendTab.RATIO) }
        binding.tabLeak.setOnClickListener { viewModel.setTrend(DmaTrendTab.LEAK) }
        binding.tabMonth.setOnClickListener { viewModel.setTrend(DmaTrendTab.MONTH) }
        binding.chipTrend7.setOnClickListener { viewModel.setTrendDays(7) }
        binding.chipTrend15.setOnClickListener { viewModel.setTrendDays(15) }
        binding.chipTrend30.setOnClickListener { viewModel.setTrendDays(30) }
        binding.chipTrendCustom.setOnClickListener { pickTrendRange() }
        binding.chipTrendYear0.setOnClickListener { viewModel.setTrendYear(0) }
        binding.chipTrendYear1.setOnClickListener { viewModel.setTrendYear(1) }
        binding.chipTrendYear2.setOnClickListener { viewModel.setTrendYear(2) }
        binding.tabHistDaily.setOnClickListener { viewModel.setHistory(DmaHistoryKind.DAILY) }
        binding.tabHistLeak.setOnClickListener { viewModel.setHistory(DmaHistoryKind.LEAK) }
        binding.tabHistNrw.setOnClickListener { viewModel.setHistory(DmaHistoryKind.NRW) }
        binding.tabHistFlow.setOnClickListener { viewModel.setHistory(DmaHistoryKind.FLOW) }
        binding.chipHist7.setOnClickListener { viewModel.setHistoryDays(7) }
        binding.chipHist15.setOnClickListener { viewModel.setHistoryDays(15) }
        binding.chipHist30.setOnClickListener { viewModel.setHistoryDays(30) }
        binding.chipHistCustom.setOnClickListener { pickHistoryRange() }
        binding.chipHistYear0.setOnClickListener { viewModel.setHistoryYear(0) }
        binding.chipHistYear1.setOnClickListener { viewModel.setHistoryYear(1) }
        binding.chipHistYear2.setOnClickListener { viewModel.setHistoryYear(2) }
        binding.chipFlow0.setOnClickListener { viewModel.setFlowPreset(0) }
        binding.chipFlow1.setOnClickListener { viewModel.setFlowPreset(1) }
        binding.chipFlow2.setOnClickListener { viewModel.setFlowPreset(2) }
        binding.chipFlowCustom.setOnClickListener { pickFlowDate() }
        binding.tvFlowDate.setOnClickListener { pickFlowDate() }
        binding.btnLoadMore.setOnClickListener { viewModel.loadMoreHistory() }
        bindRange(binding.trendRange.tvStartDate, binding.trendRange.tvEndDate, { it.trendStart to it.trendEnd }) { start, end ->
            viewModel.setTrendRange(start, end)
        }
        bindRange(binding.historyRange.tvStartDate, binding.historyRange.tvEndDate, { it.historyStart to it.historyEnd }) { start, end ->
            viewModel.setHistoryRange(start, end)
        }
        MpLineCharts.setup(binding.chartTrend)
        var downX = 0f
        binding.chartTrend.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> downX = event.x
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (abs(dx) > 120f) viewModel.stepTrend(if (dx < 0) 1 else -1)
                }
            }
            false
        }
        lifecycleScope.launch {
            viewModel.ui.collect { render(it) }
        }
        decodeDmaPartition(intent.getStringExtra(DmaNavigator.EXTRA_PARTITION).orEmpty())
            ?.let { viewModel.load(it) }
    }

    private fun pickTrendRange() {
        val ui = viewModel.ui.value
        InstDateRange.pick(this, getString(R.string.dma_pick_range), InstDateRange.parseDay(ui.trendStart), InstDateRange.parseDay(ui.trendEnd)) { start, end ->
            val startText = InstDateRange.formatDay(start) ?: return@pick
            val endText = InstDateRange.formatDay(end) ?: return@pick
            viewModel.setTrendRange(startText, endText)
        }
    }

    private fun pickHistoryRange() {
        val ui = viewModel.ui.value
        InstDateRange.pick(this, getString(R.string.dma_pick_range), InstDateRange.parseDay(ui.historyStart), InstDateRange.parseDay(ui.historyEnd)) { start, end ->
            val startText = InstDateRange.formatDay(start) ?: return@pick
            val endText = InstDateRange.formatDay(end) ?: return@pick
            viewModel.setHistoryRange(startText, endText)
        }
    }

    private fun pickFlowDate() {
        DmaDates.pickDay(this, viewModel.ui.value.flowDate) { viewModel.setFlowDate(it) }
    }

    private fun bindRange(
        startView: TextView,
        endView: TextView,
        current: (DmaDetailUi) -> Pair<String, String>,
        onPicked: (String, String) -> Unit,
    ) {
        val open = {
            val (start, end) = current(viewModel.ui.value)
            InstDateRange.pick(this, getString(R.string.dma_pick_range), InstDateRange.parseDay(start), InstDateRange.parseDay(end)) { startMillis, endMillis ->
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
        binding.tvSubTitle.text = getString(R.string.dma_detail_sub, ui.dataDate.ifBlank { "—" })
        binding.tvName.text = ui.name
        renderStatus(ui.status)
        showTag(binding.tagCode, ui.code)
        showTag(binding.tagLevel, ui.levelName)
        showTag(binding.tagParent, ui.parentName.takeIf { it.isNotBlank() }?.let { getString(R.string.dma_belong, it) })
        showTag(binding.tagUser, ui.userName)
        showTag(binding.tagNight, ui.nightWindow.takeIf { it.isNotBlank() }?.let { getString(R.string.dma_night_window, it) })
        val shortDate = if (ui.dataDate.length >= 10) ui.dataDate.substring(5) else ui.dataDate
        binding.tvLatest.text = getString(R.string.dma_latest, shortDate.ifBlank { "—" })
        tileAdapter.submit(ui.tiles)
        renderTrend(ui)
        renderHistory(ui)
    }

    private fun showTag(view: TextView, text: String?) {
        view.text = text.orEmpty()
        view.isVisible = !text.isNullOrBlank()
    }

    private fun renderStatus(status: Int) {
        val (text, color, bg) = when (status) {
            0 -> Triple(R.string.dma_normal, R.color.success, R.color.dma_status_normal_bg)
            1 -> Triple(R.string.dma_warn, R.color.warning, R.color.dma_status_warn_bg)
            2 -> Triple(R.string.dma_severe, R.color.crimson, R.color.dma_status_severe_bg)
            else -> Triple(R.string.dma_unknown, R.color.text_secondary, R.color.dma_tag)
        }
        binding.tvStatus.setText(text)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, color))
        ViewCompat.setBackgroundTintList(binding.statusBadge, ColorStateList.valueOf(ContextCompat.getColor(this, bg)))
        ViewCompat.setBackgroundTintList(binding.vStatusDot, ColorStateList.valueOf(ContextCompat.getColor(this, color)))
    }

    private fun renderTrend(ui: DmaDetailUi) {
        val month = ui.trend == DmaTrendTab.MONTH
        styleChip(binding.tabDay, ui.trend == DmaTrendTab.DAY)
        styleChip(binding.tabNight, ui.trend == DmaTrendTab.NIGHT)
        styleChip(binding.tabMin, ui.trend == DmaTrendTab.MIN_NIGHT)
        styleChip(binding.tabRatio, ui.trend == DmaTrendTab.RATIO)
        styleChip(binding.tabLeak, ui.trend == DmaTrendTab.LEAK)
        styleChip(binding.tabMonth, month)
        binding.btnTrendPrev.alpha = if (ui.trend == DmaTrendTab.DAY) 0.25f else 1f
        binding.btnTrendNext.alpha = if (month) 0.25f else 1f
        binding.trendDayRow.isVisible = !month
        binding.trendYearRow.isVisible = month
        binding.trendRange.root.isVisible = !month && ui.trendDays == null
        styleChip(binding.chipTrend7, ui.trendDays == 7)
        styleChip(binding.chipTrend15, ui.trendDays == 15)
        styleChip(binding.chipTrend30, ui.trendDays == 30)
        styleChip(binding.chipTrendCustom, ui.trendDays == null)
        bindYears(listOf(binding.chipTrendYear0, binding.chipTrendYear1, binding.chipTrendYear2), ui.years, ui.trendYear)
        InstDateRange.bind(binding.trendRange.tvStartDate, binding.trendRange.tvEndDate, InstDateRange.parseDay(ui.trendStart), InstDateRange.parseDay(ui.trendEnd))
        binding.tvRangeLabel.text = ui.rangeLabel
        binding.tvChartNote.setText(noteOf(ui.trend))
        val colors = when (ui.trend) {
            DmaTrendTab.NIGHT -> listOf(color(R.color.dma_value_purple), color(R.color.warning))
            DmaTrendTab.RATIO -> listOf(color(R.color.dma_value_blue), color(R.color.success))
            DmaTrendTab.MONTH -> listOf(color(R.color.crimson), color(R.color.warning))
            DmaTrendTab.LEAK -> listOf(color(R.color.crimson))
            DmaTrendTab.MIN_NIGHT -> listOf(color(R.color.warning))
            else -> listOf(color(R.color.dma_value_blue))
        }
        MpLineCharts.bindLines(
            binding.chartTrend,
            ui.chartLabels,
            ui.chartLines.map { it.name to it.values },
            colors,
            dualAxis = ui.trend == DmaTrendTab.NIGHT && ui.chartLines.size > 1,
        )
        binding.tvTrendEmpty.isVisible = ui.chartLines.isEmpty()
        binding.chartTrend.isVisible = ui.chartLines.isNotEmpty()
    }

    private fun renderHistory(ui: DmaDetailUi) {
        styleChip(binding.tabHistDaily, ui.historyKind == DmaHistoryKind.DAILY)
        styleChip(binding.tabHistLeak, ui.historyKind == DmaHistoryKind.LEAK)
        styleChip(binding.tabHistNrw, ui.historyKind == DmaHistoryKind.NRW)
        styleChip(binding.tabHistFlow, ui.historyKind == DmaHistoryKind.FLOW)
        binding.historyDayRow.isVisible = ui.historyKind == DmaHistoryKind.DAILY
        binding.historyYearRow.isVisible = ui.historyKind == DmaHistoryKind.LEAK || ui.historyKind == DmaHistoryKind.NRW
        binding.flowRow.isVisible = ui.historyKind == DmaHistoryKind.FLOW
        binding.historyRange.root.isVisible = ui.historyKind == DmaHistoryKind.DAILY && ui.historyDays == null
        binding.tvFlowDate.isVisible = ui.historyKind == DmaHistoryKind.FLOW && ui.flowCustom
        styleChip(binding.chipHist7, ui.historyDays == 7)
        styleChip(binding.chipHist15, ui.historyDays == 15)
        styleChip(binding.chipHist30, ui.historyDays == 30)
        styleChip(binding.chipHistCustom, ui.historyDays == null)
        bindYears(listOf(binding.chipHistYear0, binding.chipHistYear1, binding.chipHistYear2), ui.years, ui.historyYear)
        ui.flowDates.getOrNull(0)?.let { binding.chipFlow0.text = "${shortDay(it)}（昨日）" }
        ui.flowDates.getOrNull(1)?.let { binding.chipFlow1.text = shortDay(it) }
        ui.flowDates.getOrNull(2)?.let { binding.chipFlow2.text = shortDay(it) }
        styleChip(binding.chipFlow0, !ui.flowCustom && ui.flowDate == ui.flowDates.getOrNull(0))
        styleChip(binding.chipFlow1, !ui.flowCustom && ui.flowDate == ui.flowDates.getOrNull(1))
        styleChip(binding.chipFlow2, !ui.flowCustom && ui.flowDate == ui.flowDates.getOrNull(2))
        styleChip(binding.chipFlowCustom, ui.flowCustom)
        binding.tvFlowDate.text = ui.flowDate
        InstDateRange.bind(binding.historyRange.tvStartDate, binding.historyRange.tvEndDate, InstDateRange.parseDay(ui.historyStart), InstDateRange.parseDay(ui.historyEnd))
        binding.tvHistoryCaption.text = ui.historyCaption
        historyAdapter.submit(ui.rows)
        binding.tvHistoryEmpty.isVisible = ui.rows.isEmpty()
        binding.btnLoadMore.isVisible = ui.historyHasMore
    }

    private fun bindYears(views: List<TextView>, years: List<Int>, selected: Int) {
        val current = years.lastOrNull()
        views.forEachIndexed { index, view ->
            val year = years.getOrNull(index)
            view.isVisible = year != null
            if (year != null) {
                view.text = if (year == current) getString(R.string.dma_year_current, year) else getString(R.string.dma_year, year)
                styleChip(view, year == selected)
            }
        }
    }

    private fun noteOf(tab: DmaTrendTab): Int = when (tab) {
        DmaTrendTab.DAY -> R.string.dma_note_day
        DmaTrendTab.NIGHT -> R.string.dma_note_night
        DmaTrendTab.MIN_NIGHT -> R.string.dma_note_min
        DmaTrendTab.RATIO -> R.string.dma_note_ratio
        DmaTrendTab.LEAK -> R.string.dma_note_leak
        DmaTrendTab.MONTH -> R.string.dma_note_month
    }

    private fun styleChip(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_primary_button else R.drawable.bg_dma_pill)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_secondary))
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun shortDay(day: String): String = if (day.length >= 10) day.substring(5) else day
}

private class TileAdapter(
    private val onClick: (DmaTileUi) -> Unit,
) : RecyclerView.Adapter<TileAdapter.Holder>() {
    private var items: List<DmaTileUi> = emptyList()

    fun submit(list: List<DmaTileUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDmaTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.binding.tvLabel.text = item.label
        holder.binding.tvValue.text = item.value
        holder.binding.tvUnit.text = item.unit
        holder.binding.tvValue.setTextColor(ContextCompat.getColor(context, toneColor(item.tone)))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemDmaTileBinding) : RecyclerView.ViewHolder(binding.root)
}

private class HistoryAdapter(
    private val onDevice: (Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<DmaHistoryRowUi> = emptyList()

    fun submit(list: List<DmaHistoryRowUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = if (items[position].deviceIndex == null) TYPE_DATE else TYPE_DEVICE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DEVICE) {
            DeviceHolder(ItemDmaFlowDeviceBinding.inflate(inflater, parent, false))
        } else {
            DateHolder(ItemDmaHistoryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val last = position == items.lastIndex
        when (holder) {
            is DateHolder -> holder.bind(item, last)
            is DeviceHolder -> holder.bind(item, last, onDevice)
        }
    }

    class DateHolder(private val binding: ItemDmaHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DmaHistoryRowUi, last: Boolean) {
            binding.tvTitle.text = item.title
            binding.tvSub.text = item.subtitle
            binding.tvSub.isVisible = item.subtitle.isNotBlank()
            binding.vDivider.isVisible = !last
            val cells = listOf(
                binding.cell0, binding.cell1, binding.cell2, binding.cell3, binding.cell4, binding.cell5,
                binding.cell6, binding.cell7, binding.cell8, binding.cell9, binding.cell10, binding.cell11,
            )
            cells.forEachIndexed { index, cell ->
                val data = item.cells.getOrNull(index)
                cell.root.isVisible = data != null
                if (data != null) bindCell(cell, data.label, data.value, data.tone)
            }
            binding.rowExtra1.isVisible = item.cells.size > 6
            binding.rowExtra2.isVisible = item.cells.size > 9
        }
    }

    class DeviceHolder(private val binding: ItemDmaFlowDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DmaHistoryRowUi, last: Boolean, onDevice: (Int) -> Unit) {
            binding.tvDirection.text = item.title
            binding.tvName.text = item.subtitle
            binding.tvMeta.text = item.meta
            binding.tvMeta.isVisible = item.meta.isNotBlank()
            binding.vDivider.isVisible = !last
            val cells = listOf(binding.cell0, binding.cell1, binding.cell2, binding.cell3)
            cells.forEachIndexed { index, cell ->
                val data = item.cells.getOrNull(index)
                cell.root.isVisible = data != null
                if (data != null) bindCell(cell, data.label, data.value, data.tone)
            }
            binding.root.setOnClickListener { item.deviceIndex?.let(onDevice) }
        }
    }

    private companion object {
        const val TYPE_DATE = 0
        const val TYPE_DEVICE = 1
    }
}

private fun bindCell(cell: ItemDmaHistoryCellBinding, label: String, value: String, tone: DmaValueTone) {
    cell.tvLabel.text = label
    cell.tvValue.text = value
    cell.tvValue.setTextColor(ContextCompat.getColor(cell.root.context, toneColor(tone)))
}

private class GridLineDecoration(density: Float) : RecyclerView.ItemDecoration() {
    private val gap = density.toInt().coerceAtLeast(1)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val span = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 4
        val column = position % span
        val lastRowStart = ((state.itemCount - 1) / span) * span
        if (column < span - 1) outRect.right = gap
        if (position < lastRowStart) outRect.bottom = gap
    }
}

private fun toneColor(tone: DmaValueTone): Int = when (tone) {
    DmaValueTone.BLUE -> R.color.dma_value_blue
    DmaValueTone.PURPLE -> R.color.dma_value_purple
    DmaValueTone.NAVY -> R.color.navy
    DmaValueTone.GREEN -> R.color.success
    DmaValueTone.YELLOW -> R.color.dma_value_yellow
    DmaValueTone.ORANGE -> R.color.warning
    DmaValueTone.RED -> R.color.crimson
    DmaValueTone.DEFAULT -> R.color.navy
}
