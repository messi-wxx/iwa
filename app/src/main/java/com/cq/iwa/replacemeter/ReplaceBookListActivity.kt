package com.cq.iwa.replacemeter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.SyncProgressDialog
import com.cq.iwa.databinding.ActivityBookListBinding
import com.cq.iwa.databinding.ItemBookBinding
import com.cq.iwa.feature.replacemeter.ui.ReplaceBookItemUi
import com.cq.iwa.feature.replacemeter.ui.ReplaceBookListViewModel
import com.cq.iwa.feature.replacemeter.ui.ReplaceMeterItemUi
import com.cq.iwa.readmeter.NfcHelper
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReplaceBookListActivity : IwaBaseActivity<ActivityBookListBinding>() {

    private val viewModel: ReplaceBookListViewModel by viewModels()
    private lateinit var nfcHelper: NfcHelper
    private var syncDialog: SyncProgressDialog? = null
    private var handledSyncResult = false
    private val adapter = ReplaceBookAdapter(
        onClick = { item ->
            val state = (viewModel.uiState.value as? UiState.Success)?.data
            when {
                state?.selecting == true -> viewModel.toggleBook(item.taskId)
                !item.downloaded -> showToast(getString(R.string.readmeter_book_not_downloaded))
                else -> ReplaceMeterNavigator.openMeterList(this, item.taskId)
            }
        },
        onLongClick = {
            viewModel.toggleSelectMode(true)
            true
        },
    )

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityBookListBinding =
        ActivityBookListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        nfcHelper = NfcHelper(this)
        observeUiEvents(viewModel)
        binding.tvTitle.setText(R.string.replacemeter_title)
        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(onRefresh = { viewModel.refreshCatalog() })
        binding.btnBack.setOnClickListener { handleBack() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
        binding.btnSearch.setOnClickListener {
            binding.etSearch.isVisible = !binding.etSearch.isVisible
        }
        binding.btnSync.setOnClickListener {
            val state = viewModel.uiState.value
            val data = (state as? UiState.Success)?.data
            when {
                state is UiState.Empty || data?.books.isNullOrEmpty() -> viewModel.refreshCatalog()
                data?.selecting == true -> viewModel.syncSelected()
                else -> viewModel.toggleSelectMode(true)
            }
        }
        binding.btnSelectAll.setOnClickListener { viewModel.toggleAll() }
        binding.btnCancelSelect.setOnClickListener { viewModel.toggleSelectMode(false) }
        binding.btnDoSync.setOnClickListener { viewModel.syncSelected() }
        binding.cbNetMeter.isVisible = false
        binding.etSearch.hint = getString(R.string.replacemeter_search_hint)
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else {
                false
            }
        }

        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            onSuccess = { ui ->
                adapter.submit(ui.books, ui.selecting)
                binding.syncBar.isVisible = ui.selecting
                binding.cbNetMeter.isVisible = false
                binding.refreshLayout.finishSmart()
            },
            onEmpty = {
                binding.refreshLayout.finishSmart()
                binding.stateLayout.showEmpty(
                    getString(R.string.replacemeter_empty_hint),
                    showRetry = true,
                    retryText = getString(R.string.replacemeter_fetch_catalog),
                )
            },
            onError = { binding.refreshLayout.finishSmart() },
            onRetry = {
                val empty = viewModel.uiState.value is UiState.Empty ||
                    (viewModel.uiState.value as? UiState.Success)?.data?.books.isNullOrEmpty()
                if (empty) viewModel.refreshCatalog() else viewModel.load()
            },
        )
        lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                    !viewModel.progress.value.running
                ) {
                    viewModel.load()
                }
            },
        )
        observeProgress()
        viewModel.load()
    }

    private fun handleBack() {
        val selecting = (viewModel.uiState.value as? UiState.Success)?.data?.selecting == true
        if (selecting) {
            viewModel.toggleSelectMode(false)
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enable()
    }

    override fun onPause() {
        nfcHelper.disable()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcHelper.readMeterCode(intent)?.let(::openMetersFromNfc)
    }

    private fun observeProgress() {
        collectUi(viewModel.progress) { progress ->
            if (progress.running) {
                handledSyncResult = false
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                hideLoading()
                val dialog = syncDialog ?: SyncProgressDialog(this).also { syncDialog = it }
                dialog.update(
                    title = progress.title,
                    taskName = progress.taskName,
                    totalProgress = progress.totalProgress,
                    tip = progress.tip,
                    percent = progress.percent,
                )
            } else if (progress.finished) {
                if (handledSyncResult) {
                    viewModel.consumeProgress()
                    return@collectUi
                }
                handledSyncResult = true
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                syncDialog?.dismiss()
                syncDialog = null
                hideLoading()
                if (progress.errors.isNotEmpty()) {
                    IwaDialogs.message(
                        context = this,
                        title = getString(R.string.readmeter_sync_failed),
                        message = progress.errors.joinToString("\n"),
                    )
                } else if (progress.tip.isNotBlank() || progress.message.isNotBlank()) {
                    showToast(
                        when {
                            progress.catalogOnly -> progress.tip.ifBlank { progress.message }
                            else -> getString(R.string.readmeter_sync_success)
                        },
                    )
                }
                if (!progress.catalogOnly) {
                    viewModel.toggleSelectMode(false)
                }
                viewModel.consumeProgress()
            }
        }
    }

    private fun doSearch() {
        val key = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (key.isBlank()) return
        viewModel.search(key) { list ->
            if (list.isEmpty()) {
                showToast(getString(R.string.readmeter_no_result))
            } else {
                showMeterPick(list)
            }
        }
    }

    private fun openMetersFromNfc(code: String) {
        viewModel.search(code) { list ->
            when {
                list.isEmpty() -> showToast(getString(R.string.readmeter_nfc_not_found))
                list.size == 1 -> openReplaceable(list[0])
                else -> showMeterPick(list)
            }
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

    private fun <T> collectUi(flow: StateFlow<T>, block: (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                flow.collect(block)
            }
        }
    }
}

private class ReplaceBookAdapter(
    private val onClick: (ReplaceBookItemUi) -> Unit,
    private val onLongClick: (ReplaceBookItemUi) -> Boolean,
) : RecyclerView.Adapter<ReplaceBookAdapter.Holder>() {

    private var items: List<ReplaceBookItemUi> = emptyList()
    private var selecting = false

    fun submit(list: List<ReplaceBookItemUi>, selecting: Boolean) {
        items = list
        this.selecting = selecting
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.binding.tvName.text = item.taskName
        holder.binding.tvProgress.text = if (item.downloaded) {
            ctx.getString(
                R.string.replacemeter_book_progress,
                item.total,
                item.unfinished,
                item.finished,
            )
        } else {
            ctx.getString(R.string.readmeter_not_downloaded)
        }
        holder.binding.tvState.text = if (item.taskState == 1) {
            ctx.getString(R.string.readmeter_done)
        } else {
            ctx.getString(R.string.readmeter_in_progress)
        }
        holder.binding.cbSelect.isVisible = selecting
        holder.binding.cbSelect.setOnCheckedChangeListener(null)
        holder.binding.cbSelect.isChecked = item.selected
        holder.binding.cbSelect.setOnCheckedChangeListener { _, _ -> onClick(item) }
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item) }
    }

    class Holder(val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root)
}
