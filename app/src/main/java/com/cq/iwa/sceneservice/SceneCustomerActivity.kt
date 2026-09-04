package com.cq.iwa.sceneservice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneCustomerBinding
import com.cq.iwa.databinding.ItemSceneFunctionBinding
import com.cq.iwa.feature.sceneservice.network.SceneCustomerDto
import com.cq.iwa.feature.sceneservice.ui.SceneCustomerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneCustomerActivity : IwaBaseActivity<ActivitySceneCustomerBinding>() {

    private val viewModel: SceneCustomerViewModel by viewModels()
    private val adapter = SceneCustomerAdapter { customer ->
        setResult(
            SceneServiceNavigator.RESULT_CUSTOMER,
            Intent()
                .putExtra("customerText", customer.text.orEmpty())
                .putExtra("customerValue", customer.value.orEmpty()),
        )
        finish()
    }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneCustomerBinding =
        ActivitySceneCustomerBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        binding.btnQuery.setOnClickListener { search() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            showLoadingOverlay = false,
            onSuccess = { adapter.submit(it) },
            onRetry = { search() },
        )
        viewModel.query("")
    }

    private fun search() {
        viewModel.query(binding.etSearch.text?.toString().orEmpty())
    }
}

private class SceneCustomerAdapter(
    private val onClick: (SceneCustomerDto) -> Unit,
) : RecyclerView.Adapter<SceneCustomerAdapter.Holder>() {

    private var items: List<SceneCustomerDto> = emptyList()

    fun submit(list: List<SceneCustomerDto>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSceneFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.text.orEmpty()
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemSceneFunctionBinding) : RecyclerView.ViewHolder(binding.root)
}
