package com.cq.iwa.urgepayment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityUrgeTaskListBinding
import com.cq.iwa.databinding.ItemUrgeMeterBinding
import com.cq.iwa.feature.urgepayment.ui.UrgeMeterListViewModel
import com.cq.iwa.feature.urgepayment.ui.UrgeMeterUi
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UrgeMeterListActivity : IwaBaseActivity<ActivityUrgeTaskListBinding>() {

    private val viewModel: UrgeMeterListViewModel by viewModels()
    private val adapter = UrgeMeterAdapter { item ->
        UrgePaymentNavigator.openDetail(this, item.clientCode)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityUrgeTaskListBinding =
        ActivityUrgeTaskListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val bookId = intent.getIntExtra(UrgePaymentNavigator.EXTRA_BOOK_ID, 0)
        val taskName = intent.getStringExtra(UrgePaymentNavigator.EXTRA_TASK_NAME).orEmpty()
        binding.tvTitle.text = taskName.ifBlank { getString(R.string.urge_title) }
        viewModel.onSearchClients = { items ->
            showUrgeSearchDialog(this, items) { selected ->
                UrgePaymentNavigator.openDetail(this, selected.clientCode)
            }
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnQuery.setOnClickListener { search() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        binding.refreshLayout.bindSmartRefresh(onRefresh = { viewModel.refresh() })
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            showLoadingOverlay = false,
            onSuccess = { ui ->
                if (ui.taskName.isNotBlank()) binding.tvTitle.text = ui.taskName
                adapter.submit(ui.meters)
                binding.refreshLayout.finishSmart()
            },
            onEmpty = { binding.refreshLayout.finishSmart() },
            onError = { binding.refreshLayout.finishSmart() },
            onRetry = { viewModel.load(bookId, taskName) },
        )
        if (bookId <= 0) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        viewModel.load(bookId, taskName)
    }

    private fun search() {
        viewModel.search(binding.etSearch.text?.toString().orEmpty())
    }
}

private class UrgeMeterAdapter(
    private val onClick: (UrgeMeterUi) -> Unit,
) : RecyclerView.Adapter<UrgeMeterAdapter.Holder>() {

    private var items: List<UrgeMeterUi> = emptyList()

    fun submit(list: List<UrgeMeterUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeMeterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvAddress.text = item.address
        holder.binding.tvFee.isVisible = item.feeText.isNotBlank()
        holder.binding.tvFee.text = item.feeText
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemUrgeMeterBinding) : RecyclerView.ViewHolder(binding.root)
}
