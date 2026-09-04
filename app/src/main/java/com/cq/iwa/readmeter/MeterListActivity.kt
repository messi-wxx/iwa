package com.cq.iwa.readmeter

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
import com.cq.iwa.databinding.ActivityMeterListBinding
import com.cq.iwa.databinding.ItemMeterBinding
import com.cq.iwa.feature.readmeter.MeterState
import com.cq.iwa.feature.readmeter.ui.MeterFilter
import com.cq.iwa.feature.readmeter.ui.MeterItemUi
import com.cq.iwa.feature.readmeter.ui.MeterListViewModel
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MeterListActivity : IwaBaseActivity<ActivityMeterListBinding>() {

    private val viewModel: MeterListViewModel by viewModels()
    private lateinit var nfcHelper: NfcHelper
    private var platform: String = "edc"
    private val adapter = MeterAdapter(
        onClick = { item ->
            MeterNavigator.openDetail(this, item.tableId, platform)
        },
        onLongClick = { item ->
            confirmDelete(item)
        },
    )

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityMeterListBinding =
        ActivityMeterListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        observeUiEvents(viewModel)
        platform = intent.getStringExtra(MeterNavigator.EXTRA_PLATFORM).orEmpty()
        val taskId = intent.getStringExtra(MeterNavigator.EXTRA_TASK_ID).orEmpty()
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
        binding.chipAll.setOnClickListener { viewModel.setFilter(MeterFilter.ALL) }
        binding.chipUnread.setOnClickListener { viewModel.setFilter(MeterFilter.UNREAD) }
        binding.chipRead.setOnClickListener { viewModel.setFilter(MeterFilter.READ) }
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
            } else false
        }
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            onSuccess = { ui ->
                binding.tvTitle.text = ui.taskName
                adapter.submit(ui.meters)
                bindFilter(ui.filter)
                binding.tvGroup.isVisible = ui.groups.isNotEmpty()
                binding.tvGroup.text = ui.groupName.ifBlank { getString(R.string.readmeter_all_groups) }
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
        intent.getStringExtra(MeterNavigator.EXTRA_TASK_ID)?.let { viewModel.load(it, overlay = false) }
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
                    list.size == 1 -> MeterNavigator.openDetail(this, list[0].tableId, platform, true)
                    else -> showMeterPick(list, fromNfc = true)
                }
            }
        }
    }

    private fun pickGroup() {
        val ui = (viewModel.uiState.value as? com.cq.iwa.core.common.model.UiState.Success)?.data ?: return
        if (ui.groups.isEmpty()) return
        val options = ui.groups + getString(R.string.readmeter_all_groups)
        IwaDialogs.list(this, getString(R.string.readmeter_all_groups), options) { which ->
            viewModel.setGroup(options[which])
        }
    }

    private fun confirmDelete(item: MeterItemUi) {
        if (item.state <= MeterState.UNREAD) return
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.readmeter_delete_meter_title),
            message = getString(R.string.readmeter_delete_meter_message),
            confirmText = getString(R.string.readmeter_delete),
            onConfirm = { viewModel.deleteMeter(item.tableId) },
        )
    }

    private fun showMeterPick(list: List<MeterItemUi>, fromNfc: Boolean = false) {
        val labels = list.map { "${it.meterCode}  ${it.clientName}\n${it.address}" }
        IwaDialogs.list(this, getString(R.string.readmeter_search), labels) { which ->
            MeterNavigator.openDetail(this, list[which].tableId, platform, fromNfc)
        }
    }

    private fun bindFilter(filter: MeterFilter) {
        fun style(view: android.widget.TextView, selected: Boolean) {
            view.setBackgroundResource(if (selected) R.drawable.bg_chip_uploaded else R.drawable.bg_chip_unread)
            view.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.primary else R.color.text_secondary),
            )
        }
        style(binding.chipAll, filter == MeterFilter.ALL)
        style(binding.chipUnread, filter == MeterFilter.UNREAD)
        style(binding.chipRead, filter == MeterFilter.READ)
    }
}

private class MeterAdapter(
    private val onClick: (MeterItemUi) -> Unit,
    private val onLongClick: (MeterItemUi) -> Unit,
) : RecyclerView.Adapter<MeterAdapter.Holder>() {

    private var items: List<MeterItemUi> = emptyList()

    fun submit(list: List<MeterItemUi>) {
        items = list
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
        holder.binding.tvMeterCode.text = item.meterCode
        holder.binding.tvClient.text = item.clientName
        holder.binding.tvAddress.text = item.address
        val (text, bg, color) = when (item.state) {
            MeterState.READ -> Triple(
                ctx.getString(R.string.readmeter_read),
                R.drawable.bg_chip_read,
                R.color.logout,
            )
            MeterState.UPLOADED -> Triple(
                ctx.getString(R.string.readmeter_uploaded),
                R.drawable.bg_chip_uploaded,
                R.color.primary,
            )
            else -> Triple(
                ctx.getString(R.string.readmeter_unread),
                R.drawable.bg_chip_unread,
                R.color.primary,
            )
        }
        holder.binding.tvState.text = text
        holder.binding.tvState.setBackgroundResource(bg)
        holder.binding.tvState.setTextColor(ContextCompat.getColor(ctx, color))
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            if (item.state <= MeterState.UNREAD) return@setOnLongClickListener false
            onLongClick(item)
            true
        }
    }

    class Holder(val binding: ItemMeterBinding) : RecyclerView.ViewHolder(binding.root)
}
