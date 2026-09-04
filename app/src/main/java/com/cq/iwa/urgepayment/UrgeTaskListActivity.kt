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
import com.cq.iwa.databinding.ItemUrgeTaskBinding
import com.cq.iwa.feature.urgepayment.ui.UrgeTaskListViewModel
import com.cq.iwa.feature.urgepayment.ui.UrgeTaskUi
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UrgeTaskListActivity : IwaBaseActivity<ActivityUrgeTaskListBinding>() {

    private val viewModel: UrgeTaskListViewModel by viewModels()
    private val adapter = UrgeTaskAdapter { item ->
        UrgePaymentNavigator.openMeterList(this, item.bookId, item.taskName)
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityUrgeTaskListBinding =
        ActivityUrgeTaskListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
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
        binding.refreshLayout.bindSmartRefresh(onRefresh = { viewModel.load(fromRefresh = true) })
        binding.btnRefresh.setOnClickListener { viewModel.load(fromRefresh = true) }
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            showLoadingOverlay = false,
            onSuccess = { ui ->
                adapter.submit(ui.tasks)
                binding.refreshLayout.finishSmart()
            },
            onEmpty = { binding.refreshLayout.finishSmart() },
            onError = { binding.refreshLayout.finishSmart() },
            onRetry = { viewModel.load() },
        )
        viewModel.load()
    }

    private fun search() {
        viewModel.search(binding.etSearch.text?.toString().orEmpty())
    }
}

private class UrgeTaskAdapter(
    private val onClick: (UrgeTaskUi) -> Unit,
) : RecyclerView.Adapter<UrgeTaskAdapter.Holder>() {

    private var items: List<UrgeTaskUi> = emptyList()

    fun submit(list: List<UrgeTaskUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.taskName
        holder.binding.tvFeeCount.isVisible = item.feeCountText.isNotBlank()
        holder.binding.tvFeeCount.text = item.feeCountText
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemUrgeTaskBinding) : RecyclerView.ViewHolder(binding.root)
}
