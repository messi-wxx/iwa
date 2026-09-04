package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityInstMyBinding
import com.cq.iwa.feature.installation.ui.InstListViewModel
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstMyProjectsActivity : InstActivity<ActivityInstMyBinding>() {

    private val viewModel: InstListViewModel by viewModels()

    override fun inflateBinding() = ActivityInstMyBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_my_projects),
            R.menu.menu_inst_filter,
        ) { item ->
            if (item.itemId == R.id.action_filter) {
                showFilter()
                true
            } else {
                false
            }
        }
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("待办"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("催办"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("关注"))
        val adapter = InstProjectAdapter(
            onItem = {
                val browse = binding.tabLayout.selectedTabPosition != 0
                InstNavigator.openDetail(this, it, browse)
            },
            onFollow = { viewModel.toggleFollow(it.id) },
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        binding.swipeRefresh.bindSmartRefresh(onRefresh = { viewModel.load(currentCode(), overlay = false) })
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.clearFilters()
                adapter.showFollow = tab?.position == 2
                viewModel.load(currentCode())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
        lifecycleScope.launch {
            var lastStamp = -1
            viewModel.ui.collect { ui ->
                adapter.submit(ui.items)
                adapter.showFollow = ui.workBenchCode == "follow"
                binding.tvEmpty.isVisible = ui.empty
                binding.rvItems.isVisible = !ui.empty
                binding.tabLayout.getTabAt(0)?.text = "待办(${ui.pendingCount})"
                binding.tabLayout.getTabAt(1)?.text = "催办(${ui.urgeCount})"
                binding.tabLayout.getTabAt(2)?.text = "关注(${ui.followCount})"
                if (ui.listStamp != lastStamp) {
                    lastStamp = ui.listStamp
                    binding.swipeRefresh.finishSmart()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(currentCode())
    }

    private fun currentCode(): String = when (binding.tabLayout.selectedTabPosition) {
        1 -> "myurge"
        2 -> "follow"
        else -> "pending"
    }

    private fun showFilter() {
        InstFilterSheet.show(
            this,
            viewModel.code,
            viewModel.address,
            viewModel.applicantInfo,
            viewModel.type,
            viewModel.state,
            viewModel.beginTime,
            viewModel.endTime,
        ) { code, address, applicant, type, state, start, end ->
            viewModel.applyFilters(code, address, applicant, type, state, start, end)
        }
    }
}
