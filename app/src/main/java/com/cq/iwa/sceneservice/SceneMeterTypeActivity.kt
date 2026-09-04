package com.cq.iwa.sceneservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneListBinding
import com.cq.iwa.databinding.ItemSceneFunctionBinding
import com.cq.iwa.feature.sceneservice.ui.SceneMeterTypeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneMeterTypeActivity : IwaBaseActivity<ActivitySceneListBinding>() {

    private val viewModel: SceneMeterTypeViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneListBinding =
        ActivitySceneListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        binding.tvTitle.text = getString(R.string.scene_meter_type)
        binding.btnBack.setOnClickListener { finish() }
        val items = listOf(
            getString(R.string.scene_meter_common),
            getString(R.string.scene_meter_convey),
            getString(R.string.scene_meter_convey_valve),
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = SceneSimpleAdapter(items) { index ->
            val info = viewModel.deviceInfo.value
            if (info == null) return@SceneSimpleAdapter
            SceneServiceNavigator.openSingleRead(this, deviceId = 0, deviceInfo = info, replaceType = index + 1)
        }
        viewModel.load(intent.getIntExtra(SceneServiceNavigator.EXTRA_DEVICE_ID, 0))
    }
}

internal class SceneSimpleAdapter(
    private val items: List<String>,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<SceneSimpleAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSceneFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.tvName.text = items[position]
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemSceneFunctionBinding) : RecyclerView.ViewHolder(binding.root)
}
