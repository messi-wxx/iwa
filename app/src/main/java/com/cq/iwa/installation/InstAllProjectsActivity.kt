package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityInstAllBinding
import com.cq.iwa.feature.installation.ui.InstAllViewModel
import com.cq.iwa.widget.bindSmartRefresh
import com.cq.iwa.widget.finishSmart
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstAllProjectsActivity : InstActivity<ActivityInstAllBinding>() {

    private val viewModel: InstAllViewModel by viewModels()

    override fun inflateBinding() = ActivityInstAllBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_all_projects),
            R.menu.menu_inst_filter,
        ) { item ->
            if (item.itemId == R.id.action_filter) {
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
                true
            } else {
                false
            }
        }
        val adapter = InstProjectAdapter(
            onItem = { InstNavigator.openDetail(this, it, browse = true) },
            onFollow = { viewModel.toggleFollow(it.id) },
            onUrge = { project ->
                if (project.state == 3) {
                    showToast("项目已完成")
                } else {
                    IwaDialogs.input(
                        this,
                        title = getString(R.string.inst_urge),
                        hint = "请输入催办理由",
                    ) { content ->
                        if (content.isBlank()) {
                            showToast("催办理由不能为空")
                        } else {
                            viewModel.urge(project.id, content)
                        }
                    }
                }
            },
        )
        adapter.showFollow = true
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        binding.swipeRefresh.bindSmartRefresh(
            enableLoadMore = true,
            onRefresh = { viewModel.refresh(overlay = false) },
            onLoadMore = { viewModel.loadMore() },
        )
        lifecycleScope.launch {
            var lastVersion = -1
            viewModel.ui.collect { ui ->
                adapter.submit(ui.items)
                adapter.showUrge = ui.hasUrge
                binding.toolBar.tvTitle.text = getString(R.string.inst_all_projects) + "(${ui.total})"
                binding.tvEmpty.isVisible = ui.empty
                binding.rvItems.isVisible = !ui.empty
                if (ui.version != lastVersion) {
                    lastVersion = ui.version
                    binding.swipeRefresh.finishSmart(ui.hasMore, enableLoadMore = true)
                }
            }
        }
    }
}
