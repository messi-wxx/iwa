package com.cq.iwa.replacemeter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityReplaceMeterListBinding
import com.cq.iwa.databinding.ItemMeterBinding
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterFilter
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterItemUi
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterListViewModel
import com.cq.iwa.readmeter.NfcHelper
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceMeterListActivity : IwaBaseActivity<ActivityReplaceMeterListBinding>() {

    private val viewModel: ReplaceMeterListViewModel by viewModels()
    private lateinit var nfcHelper: NfcHelper
    private val adapter = ReplaceMeterAdapter { item ->
        if (item.isReplace != 1) {
            showToast(getString(R.string.replacemeter_not_replaceable))
        } else {
            ReplaceMeterNavigator.openDetail(this, item.tableId)
        }
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityReplaceMeterListBinding =
        ActivityReplaceMeterListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        observeUiEvents(viewModel)
        val taskId = intent.getStringExtra(ReplaceMeterNavigator.EXTRA_TASK_ID).orEmpty()
        binding.rvMeters.layoutManager = LinearLayoutManager(this)
        binding.rvMeters.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(
            enableLoadMore = true,
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
        )
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSearch.setOnClickListener {
            binding.etSearch.isVisible = !binding.etSearch.isVisible
        }
        binding.chipUnfinished.setOnClickListener { viewModel.setFilter(ReplaceMeterFilter.UNFINISHED) }
        binding.chipFinished.setOnClickListener { viewModel.setFilter(ReplaceMeterFilter.FINISHED) }
        binding.btnMap.setOnClickListener {
            ReplaceMeterNavigator.openMap(this, taskId)
        }
        binding.tvGroup.setOnClickListener { pickGroup() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val key = binding.etSearch.text?.toString()?.trim().orEmpty()
                if (key.isNotBlank()) {
                    viewModel.search(key) { list ->
                        if (list.isEmpty()) {
                            showToast(getString(R.string.readmeter_no_result))
                        } else {
                            showMeterPick(list)
                        }
                    }
                }
                true
            } else {
                false
            }
        }
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            onSuccess = { ui ->
                binding.tvTitle.text = ui.taskName
                adapter.submit(ui.meters, ui.filter)
                bindFilter(ui.filter, ui.unfinished, ui.finished)
                binding.tvGroup.isVisible = ui.groups.isNotEmpty()
                binding.tvGroup.text = ui.groupName.ifBlank { getString(R.string.replacemeter_all_groups) }
                binding.refreshLayout.finishSmart(ui.hasMore, enableLoadMore = true)
            },
            onEmpty = {
                binding.refreshLayout.finishSmart(hasMore = false, enableLoadMore = true)
            },
            onError = { binding.refreshLayout.finishSmart(enableLoadMore = true) },
            onRetry = { viewModel.load(taskId) },
        )
        viewModel.load(taskId)
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enable()
        intent.getStringExtra(ReplaceMeterNavigator.EXTRA_TASK_ID)?.let { viewModel.load(it, overlay = false) }
    }

    override fun onPause() {
        nfcHelper.disable()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcHelper.readMeterCode(intent)?.let { code ->
            viewModel.search(code) { list ->
                when {
                    list.isEmpty() -> showToast(getString(R.string.readmeter_nfc_not_found))
                    list.size == 1 -> openReplaceable(list[0])
                    else -> showMeterPick(list)
                }
            }
        }
    }

    private fun pickGroup() {
        val ui = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)?.data ?: return
        if (ui.groups.isEmpty()) return
        val options = ui.groups + getString(R.string.replacemeter_all_groups)
        IwaDialogs.list(this, getString(R.string.replacemeter_all_groups), options) { which ->
            viewModel.setGroup(options[which])
        }
    }

    private fun showMeterPick(list: List<ReplaceMeterItemUi>) {
        val labels = list.map { "${it.oldMeterCode}  ${it.clientCode}\n${it.address}" }
        IwaDialogs.list(this, getString(R.string.readmeter_search), labels) { which ->
            openReplaceable(list[which])
        }
    }

    private fun openReplaceable(item: ReplaceMeterItemUi) {
        if (item.isReplace != 1) {
            showToast(getString(R.string.replacemeter_not_replaceable))
            return
        }
        ReplaceMeterNavigator.openDetail(this, item.tableId)
    }

    private fun bindFilter(filter: ReplaceMeterFilter, unfinished: Int, finished: Int) {
        fun style(view: android.widget.TextView, selected: Boolean) {
            view.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else R.drawable.bg_chip_unread)
            view.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.primary else R.color.text_secondary),
            )
        }
        binding.chipUnfinished.text = getString(
            R.string.replacemeter_filter_count,
            getString(R.string.replacemeter_unfinished),
            unfinished,
        )
        binding.chipFinished.text = getString(
            R.string.replacemeter_filter_count,
            getString(R.string.replacemeter_finished),
            finished,
        )
        style(binding.chipUnfinished, filter == ReplaceMeterFilter.UNFINISHED)
        style(binding.chipFinished, filter == ReplaceMeterFilter.FINISHED)
    }
}

private class ReplaceMeterAdapter(
    private val onClick: (ReplaceMeterItemUi) -> Unit,
) : RecyclerView.Adapter<ReplaceMeterAdapter.Holder>() {

    private var items: List<ReplaceMeterItemUi> = emptyList()
    private var filter: ReplaceMeterFilter = ReplaceMeterFilter.UNFINISHED

    fun submit(list: List<ReplaceMeterItemUi>, filter: ReplaceMeterFilter) {
        items = list
        this.filter = filter
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemMeterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.binding.tvMeterCode.text = item.oldMeterCode
        holder.binding.tvClient.text = item.clientCode
        holder.binding.tvAddress.text = item.address
        val (text, bg, color) = replaceStatus(ctx, item, filter)
        holder.binding.tvState.text = text
        holder.binding.tvState.setBackgroundResource(bg)
        holder.binding.tvState.setTextColor(ContextCompat.getColor(ctx, color))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemMeterBinding) : RecyclerView.ViewHolder(binding.root)
}

private fun replaceStatus(
    ctx: android.content.Context,
    item: ReplaceMeterItemUi,
    filter: ReplaceMeterFilter,
): Triple<String, Int, Int> {
    if (item.isReplace == 0) {
        return Triple(
            ctx.getString(R.string.replacemeter_status_forbidden),
            R.drawable.bg_chip_unread,
            R.color.gray,
        )
    }
    val newBlank = if (filter == ReplaceMeterFilter.FINISHED) {
        item.newReading.isBlank()
    } else {
        item.newMeterCode.isBlank()
    }
    return when {
        item.oldReading.isBlank() && newBlank -> Triple(
            ctx.getString(R.string.replacemeter_status_none),
            R.drawable.bg_chip_unread,
            R.color.logout,
        )
        item.oldReading.isBlank() -> Triple(
            ctx.getString(R.string.replacemeter_status_old),
            R.drawable.bg_chip_read,
            R.color.warning,
        )
        newBlank -> Triple(
            ctx.getString(R.string.replacemeter_status_new),
            R.drawable.bg_chip_read,
            R.color.warning,
        )
        else -> Triple(
            ctx.getString(R.string.replacemeter_status_done),
            R.drawable.bg_chip_uploaded,
            R.color.success,
        )
    }
}
