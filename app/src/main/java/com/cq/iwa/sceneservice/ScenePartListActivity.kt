package com.cq.iwa.sceneservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneListBinding
import com.cq.iwa.databinding.ItemScenePartBinding
import com.cq.iwa.feature.sceneservice.network.ScenePartIdsDescDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScenePartListActivity : IwaBaseActivity<ActivitySceneListBinding>() {

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneListBinding =
        ActivitySceneListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.tvTitle.text = getString(R.string.scene_select_part)
        binding.btnBack.setOnClickListener { finish() }
        val parts = SceneServiceNavigator.parts(intent)
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = PartAdapter(parts) { part ->
            SceneServiceNavigator.openInputICode(this, 2, part.id)
        }
    }
}

private class PartAdapter(
    private val items: List<ScenePartIdsDescDto>,
    private val onClick: (ScenePartIdsDescDto) -> Unit,
) : RecyclerView.Adapter<PartAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemScenePartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.partDefineInfoName.orEmpty()
        holder.binding.tvCode.text = item.partDefineInfoCode.orEmpty()
        holder.binding.tvNumber.text = item.code.orEmpty()
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemScenePartBinding) : RecyclerView.ViewHolder(binding.root)
}
