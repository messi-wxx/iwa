package com.cq.iwa.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityQueryAddressBinding
import com.cq.iwa.feature.calibration.ui.PlaceSearchItemUi
import com.cq.iwa.feature.calibration.ui.QueryAddressViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QueryAddressActivity : IwaBaseActivity<ActivityQueryAddressBinding>() {

    private val viewModel: QueryAddressViewModel by viewModels()
    private val adapter = DeviceAdapter { viewModel.openItem(it) }

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityQueryAddressBinding =
        ActivityQueryAddressBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        viewModel.onOpenBuilding = { result, code ->
            MeterCalibrationNavigator.openBuilding(this, code, result)
        }
        viewModel.onChooseMultiple = { list, code ->
            IwaDialogs.list(
                this,
                getString(R.string.calibration_choose_area),
                list.map { item -> item.bookLocations.joinToString("") { loc -> loc.name.orEmpty() }.ifBlank { code } },
            ) { which ->
                viewModel.openSelected(list[which], code, allowEmpty = true)
            }
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnQuery.setOnClickListener { viewModel.search(binding.etSearch.text?.toString().orEmpty()) }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(binding.etSearch.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
        binding.rvResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val manager = recyclerView.layoutManager as LinearLayoutManager
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.loadMore()
                }
            }
        })
        collectPageState(viewModel.uiState, showLoadingOverlay = false, onSuccess = { ui ->
            adapter.submit(ui.items)
            binding.tvEmpty.isVisible = ui.empty
            binding.rvResults.isVisible = !ui.empty
        })
        val preset = intent.getStringExtra(MeterCalibrationNavigator.EXTRA_METER_CODE).orEmpty()
        if (preset.isNotBlank()) {
            binding.etSearch.setText(preset)
            binding.etSearch.setSelection(preset.length)
        }
        if (viewModel.uiState.value is UiState.Idle) {
            binding.tvEmpty.isVisible = false
        }
    }
}

private class DeviceAdapter(
    private val onClick: (String) -> Unit,
) : RecyclerView.Adapter<DeviceAdapter.Holder>() {
    private var items: List<PlaceSearchItemUi> = emptyList()

    fun submit(list: List<PlaceSearchItemUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_query_device, parent, false) as TextView
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.label.text = item.text
        holder.itemView.setOnClickListener { onClick(item.text) }
    }

    class Holder(val label: TextView) : RecyclerView.ViewHolder(label)
}
