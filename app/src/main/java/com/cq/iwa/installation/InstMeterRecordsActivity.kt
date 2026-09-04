package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityInstMeterBinding
import com.cq.iwa.feature.installation.ui.InstMeterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstMeterRecordsActivity : InstActivity<ActivityInstMeterBinding>() {

    private val viewModel: InstMeterViewModel by viewModels()

    override fun inflateBinding() = ActivityInstMeterBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        viewModel.projectId = intent.getIntExtra(InstNavigator.EXTRA_PROJECT_ID, 0)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_meters),
            R.menu.menu_inst_meter_record,
        ) { item ->
            if (item.itemId == R.id.action_search) {
                showSearch()
                true
            } else {
                false
            }
        }
        val adapter = InstMeterAdapter(onItem = { record ->
            IwaDialogs.message(
                this,
                title = "仪表详情",
                message = "仪表编号: ${record.meterNo.ifBlank { "-" }}\n${instMeterSummary(record)}",
            )
        })
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        lifecycleScope.launch {
            viewModel.ui.collect { ui -> adapter.submit(ui.items) }
        }
        viewModel.load()
    }

    private fun showSearch() {
        val meterNo = InstDialogs.field(this, "表号")
        val userNo = InstDialogs.field(this, "户号")
        val address = InstDialogs.field(this, "地址")
        InstDialogs.form(this, getString(R.string.inst_search), buildContent = { box ->
            box.addView(meterNo)
            box.addView(userNo)
            box.addView(address)
        }) {
            viewModel.load(
                meterNo.text.toString().takeIf { it.isNotBlank() },
                userNo.text.toString().takeIf { it.isNotBlank() },
                address.text.toString().takeIf { it.isNotBlank() },
            )
            true
        }
    }
}
