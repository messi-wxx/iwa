package com.cq.iwa.pipeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityPipelineTreeBinding
import com.cq.iwa.databinding.ItemPipelineTreeBinding
import com.cq.iwa.feature.pipeline.network.PipelineTreeItemDto
import com.cq.iwa.feature.pipeline.ui.PipelineTreeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PipelineTreeActivity : IwaBaseActivity<ActivityPipelineTreeBinding>() {

    private val viewModel: PipelineTreeViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background
    override fun inflateBinding() = ActivityPipelineTreeBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnParent.setOnClickListener { viewModel.back() }
        val adapter = TreeAdapter(
            onEnter = { viewModel.enter(it) },
            onDetail = { PipelineNavigator.openDetail(this, it.id) },
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                adapter.submit(ui.current)
                binding.tvPath.text = ui.title
            }
        }
        viewModel.load()
    }

    private class TreeAdapter(
        private val onEnter: (Int) -> Unit,
        private val onDetail: (PipelineTreeItemDto) -> Unit,
    ) : RecyclerView.Adapter<TreeAdapter.Holder>() {
        private var items: List<PipelineTreeItemDto> = emptyList()
        fun submit(value: List<PipelineTreeItemDto>) {
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPipelineTreeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvName.text = item.label
            holder.binding.ivType.setImageResource(
                when (item.type) {
                    1 -> R.drawable.site_icon_one
                    2 -> R.drawable.site_icon_two
                    else -> R.drawable.site_icon_three
                },
            )
            holder.itemView.setOnClickListener { onEnter(holder.adapterPosition) }
            holder.binding.ivDetail.setOnClickListener { onDetail(item) }
        }

        class Holder(val binding: ItemPipelineTreeBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
