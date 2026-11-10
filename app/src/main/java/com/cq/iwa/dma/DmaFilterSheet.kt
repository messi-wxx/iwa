package com.cq.iwa.dma

import android.view.LayoutInflater
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.R
import com.cq.iwa.databinding.DialogDmaFilterBinding
import com.cq.iwa.feature.dma.ui.DmaFilterOptions
import com.cq.iwa.feature.dma.ui.DmaListViewModel
import com.cq.iwa.feature.dma.ui.DmaOption
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object DmaFilterSheet {

    fun show(activity: DmaListActivity, viewModel: DmaListViewModel) {
        viewModel.loadFilterOptions()
        val binding = DialogDmaFilterBinding.inflate(LayoutInflater.from(activity))
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(binding.root)
        val current = viewModel.ui.value
        var levelId = current.levelNum
        var parentId = current.parentId
        fun paint(options: DmaFilterOptions) {
            bindChips(activity, binding.cgLevel, options.levels, levelId) { levelId = it }
            bindChips(activity, binding.cgParent, options.parents, parentId) { parentId = it }
        }
        paint(viewModel.options.value)
        val job: Job = activity.lifecycleScope.launch {
            viewModel.options.collect { paint(it) }
        }
        binding.btnReset.setOnClickListener {
            levelId = null
            parentId = null
            paint(viewModel.options.value)
        }
        binding.btnOk.setOnClickListener {
            val options = viewModel.options.value
            viewModel.setScope(
                levelNum = levelId,
                levelName = options.levels.firstOrNull { it.id == levelId }?.name.orEmpty(),
                parentId = parentId,
                parentName = options.parents.firstOrNull { it.id == parentId }?.name.orEmpty(),
            )
            dialog.dismiss()
        }
        dialog.setOnDismissListener { job.cancel() }
        dialog.show()
    }

    private fun bindChips(
        activity: DmaListActivity,
        group: ChipGroup,
        options: List<DmaOption>,
        selectedId: Int?,
        onPick: (Int?) -> Unit,
    ) {
        group.setOnCheckedStateChangeListener(null)
        group.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        group.addView(chip(inflater, activity.getString(R.string.dma_filter_any), null, selectedId == null))
        options.forEach { option ->
            group.addView(chip(inflater, option.name, option.id, option.id == selectedId))
        }
        group.setOnCheckedStateChangeListener { _, checkedIds ->
            val checked = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            onPick(group.findViewById<Chip>(checked).tag as? Int)
        }
    }

    private fun chip(inflater: LayoutInflater, label: String, value: Int?, checked: Boolean): Chip {
        val view = inflater.inflate(R.layout.item_dma_filter_chip, null) as Chip
        view.id = TextView.generateViewId()
        view.text = label
        view.tag = value
        view.isChecked = checked
        return view
    }
}
