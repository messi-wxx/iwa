package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityInstPendingBinding
import com.cq.iwa.feature.installation.ui.InstListViewModel
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstPendingActivity : InstActivity<ActivityInstPendingBinding>() {

    private val viewModel: InstListViewModel by viewModels()

    override fun inflateBinding() = ActivityInstPendingBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_pending_title),
            R.menu.menu_inst_pending,
        ) { item ->
            if (item.itemId == R.id.action_view_all) {
                InstNavigator.openAll(this)
                true
            } else {
                false
            }
        }
        binding.btnMy.setOnClickListener { InstNavigator.openMy(this) }
        binding.swipeRefresh.bindSmartRefresh(onRefresh = { viewModel.load("pending", overlay = false) })
        val adapter = InstProjectAdapter(onItem = { InstNavigator.openDetail(this, it) })
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        lifecycleScope.launch {
            var lastStamp = -1
            viewModel.ui.collect { ui ->
                adapter.submit(ui.items)
                binding.tvCount.text = ui.pendingCount.toString()
                binding.tvEmpty.isVisible = ui.empty
                binding.rvItems.isVisible = !ui.empty
                if (ui.listStamp != lastStamp) {
                    lastStamp = ui.listStamp
                    binding.swipeRefresh.finishSmart()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load("pending")
    }
}
