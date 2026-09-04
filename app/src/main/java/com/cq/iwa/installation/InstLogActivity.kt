package com.cq.iwa.installation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityInstLogBinding
import com.cq.iwa.feature.installation.ui.InstLogViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InstLogActivity : InstActivity<ActivityInstLogBinding>() {

    private val viewModel: InstLogViewModel by viewModels()
    private var chipsReady = false

    override fun inflateBinding() = ActivityInstLogBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        setupInstHeader(binding.toolBar, getString(R.string.inst_log))
        val adapter = InstLogAdapter()
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        val openRange = {
            val ui = viewModel.ui.value
            InstDateRange.pick(
                this,
                getString(R.string.inst_log_date),
                InstDateRange.parseDay(ui.startDate),
                InstDateRange.parseDay(ui.endDate),
            ) { start, end ->
                val startText = InstDateRange.formatDay(start) ?: return@pick
                val endText = InstDateRange.formatDay(end) ?: return@pick
                viewModel.setDates(startText, endText)
            }
        }
        binding.dateRangeBar.tvStartDate.setOnClickListener { openRange() }
        binding.dateRangeBar.tvEndDate.setOnClickListener { openRange() }
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                InstDateRange.bind(
                    binding.dateRangeBar.tvStartDate,
                    binding.dateRangeBar.tvEndDate,
                    InstDateRange.parseDay(ui.startDate),
                    InstDateRange.parseDay(ui.endDate),
                )
                adapter.submit(ui.items)
                bindTypeChips(ui.types)
            }
        }
        viewModel.setup(intent.getIntExtra(InstNavigator.EXTRA_PROJECT_ID, 0))
    }

    private fun bindTypeChips(types: List<Pair<String, String>>) {
        if (chipsReady || types.isEmpty()) return
        binding.chipGroupType.removeAllViews()
        types.forEach { (label, value) ->
            val id = value.toIntOrNull() ?: return@forEach
            if (id <= 0 || label.isBlank()) return@forEach
            val chip = layoutInflater.inflate(
                R.layout.view_inst_filter_chip,
                binding.chipGroupType,
                false,
            ) as Chip
            chip.text = label
            chip.tag = id
            chip.setOnCheckedChangeListener { _, _ ->
                if (!chipsReady) return@setOnCheckedChangeListener
                val ids = (0 until binding.chipGroupType.childCount).mapNotNull { index ->
                    val item = binding.chipGroupType.getChildAt(index) as? Chip ?: return@mapNotNull null
                    if (item.isChecked) item.tag as? Int else null
                }
                viewModel.setTypes(ids)
            }
            binding.chipGroupType.addView(chip)
        }
        chipsReady = binding.chipGroupType.childCount > 0
    }
}
