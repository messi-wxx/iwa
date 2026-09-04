package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityInstDetailBinding
import com.cq.iwa.feature.installation.network.InstJson
import com.cq.iwa.feature.installation.network.InstProjectDto
import com.cq.iwa.feature.installation.ui.InstDetailViewModel
import com.cq.iwa.feature.installation.ui.InstVFormViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstDetailActivity : InstActivity<ActivityInstDetailBinding>() {

    private val viewModel: InstDetailViewModel by viewModels()
    private val formViewModel: InstVFormViewModel by viewModels()
    private var project: InstProjectDto? = null

    override fun inflateBinding() = ActivityInstDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        observeUiEvents(formViewModel)
        project = InstJson.decode(intent.getStringExtra(InstNavigator.EXTRA_PROJECT))
        if (project == null) {
            finish()
            return
        }
        val browse = intent.getIntExtra(InstNavigator.EXTRA_TODO, 0) == 1
        binding.fabHandle.isVisible = !browse
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_detail_title),
            R.menu.menu_inst_detail,
        ) { item ->
            val current = project ?: return@setupInstHeader false
            when (item.itemId) {
                R.id.action_meter_record -> {
                    InstNavigator.openMeters(this, current.id, editable = false)
                    true
                }
                R.id.action_project_log -> {
                    InstNavigator.openLog(this, current.id)
                    true
                }
                else -> false
            }
        }
        binding.tvCode.text = project?.code
        binding.tvState.text = project?.stateDesc
        binding.ivProcessOverview.setOnClickListener { viewModel.loadOverview(project!!.id) }
        val sketchAdapter = InstSketchAdapter()
        val tableAdapter = InstTableAdapter { table ->
            if (table.datas.isEmpty()) {
                showToast("暂无数据")
            } else if (table.tableName == "仪表档案") {
                InstNavigator.openMeters(this, project!!.id, editable = false)
            } else {
                InstDialogs.table(this, table)
            }
        }
        val timelineAdapter = InstTimelineAdapter { taskId, title ->
            val form = viewModel.formOf(taskId)
            if (form == null) {
                showToast("暂无表单数据")
                return@InstTimelineAdapter
            }
            val schema = form.formMobileJsonSchema.orEmpty()
            val data = form.formData.orEmpty()
            if (schema.isBlank()) {
                showToast("暂未配置表单jsonSchema")
                return@InstTimelineAdapter
            }
            InstFormDialog(
                this,
                title,
                schema,
                data,
                project!!.id,
                formViewModel,
            ) { message -> showToast(message) }.show()
        }
        binding.rvSketch.layoutManager = LinearLayoutManager(this)
        binding.rvSketch.adapter = sketchAdapter
        binding.rvTables.layoutManager = LinearLayoutManager(this)
        binding.rvTables.adapter = tableAdapter
        binding.rvTimeline.layoutManager = LinearLayoutManager(this)
        binding.rvTimeline.adapter = timelineAdapter
        binding.fabHandle.setOnClickListener { handleFab() }
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                binding.tvCurrent.text = ui.currentName
                binding.currentProcessView.isVisible = ui.currentName.isNotBlank()
                sketchAdapter.submit(ui.sketch?.sketchFieldsList.orEmpty())
                tableAdapter.submit(ui.sketch?.dataList.orEmpty())
                timelineAdapter.submit(ui.timeline)
                binding.fabHandle.text = ui.fabText
                val overview = ui.overview
                val xml = overview?.xml
                if (overview != null) {
                    viewModel.consumeOverview()
                    if (xml.isNullOrBlank()) {
                        showToast("未获取到流程数据")
                    } else {
                        InstOverviewDialog(
                            this@InstDetailActivity,
                            xml,
                            overview.highlightedNodeIds,
                            overview.activeNodeIds,
                        ).show()
                    }
                }
            }
        }
        viewModel.load(project!!.id, project!!.taskId)
    }

    private fun handleFab() {
        val project = project ?: return
        if (project.taskId.isBlank()) {
            showToast("taskId为空")
            return
        }
        if (binding.fabHandle.text == "去认领") {
            viewModel.claim(project.taskId, project.id)
            return
        }
        val detail = viewModel.ui.value.taskDetail
        val schema = detail?.config?.form?.mobileJsonSchema
        if (schema.isNullOrBlank() || schema == "{}") {
            showToast("暂未配置表单jsonSchema")
            return
        }
        val actions = detail.config?.actionConfig
        if (actions == null) {
            showToast("获取配置按钮失败")
            return
        }
        InstNavigator.openVForm(
            this,
            project.id,
            project.taskId,
            schema,
            detail.detail?.lastFormData,
            binding.tvCurrent.text.toString(),
            actions,
        )
        finish()
    }
}
