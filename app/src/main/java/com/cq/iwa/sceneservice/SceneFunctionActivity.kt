package com.cq.iwa.sceneservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneListBinding
import com.cq.iwa.databinding.ItemSceneFunctionBinding
import com.cq.iwa.feature.sceneservice.network.SceneQueryResultDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneFunctionActivity : IwaBaseActivity<ActivitySceneListBinding>() {

    private lateinit var queryResult: SceneQueryResultDto

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneListBinding =
        ActivitySceneListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        val result = SceneServiceNavigator.queryResult(intent)
        if (result == null) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        queryResult = result
        binding.tvTitle.text = getString(R.string.scene_functions)
        binding.btnBack.setOnClickListener { finish() }
        val items = listOf(
            SceneFunctionItem(getString(R.string.scene_replace_part), "epo"),
            SceneFunctionItem(getString(R.string.scene_update_part_icode), "epo"),
            SceneFunctionItem(getString(R.string.scene_update_product_icode), "epo"),
            SceneFunctionItem(getString(R.string.scene_single_read), "edc"),
            SceneFunctionItem(getString(R.string.scene_replace_meter), "edc"),
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = SceneFunctionAdapter(items) { index -> onFunction(index) }
    }

    private fun onFunction(index: Int) {
        when (index) {
            0 -> {
                val product = queryResult.epoProductInfo
                if (product == null) showToast(getString(R.string.scene_unsupported))
                else SceneServiceNavigator.openProductDetail(this, product)
            }
            1 -> {
                val parts = queryResult.epoProductInfo?.partIdsDesc
                when {
                    queryResult.epoProductInfo == null -> showToast(getString(R.string.scene_unsupported))
                    parts.isNullOrEmpty() -> showToast(getString(R.string.scene_no_parts))
                    else -> SceneServiceNavigator.openPartList(this, parts)
                }
            }
            2 -> {
                val product = queryResult.epoProductInfo
                if (product == null) showToast(getString(R.string.scene_unsupported))
                else SceneServiceNavigator.openInputICode(this, 1, product.id)
            }
            3 -> {
                if (queryResult.edcDeviceInfo == null) showToast(getString(R.string.scene_unsupported))
                else SceneServiceNavigator.openSingleRead(this, queryResult.edcId)
            }
            4 -> {
                if (queryResult.edcDeviceInfo == null) showToast(getString(R.string.scene_unsupported))
                else SceneServiceNavigator.openMeterType(this, queryResult.edcId)
            }
        }
    }
}

private data class SceneFunctionItem(val name: String, val platform: String)

private class SceneFunctionAdapter(
    private val items: List<SceneFunctionItem>,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<SceneFunctionAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSceneFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvPlatform.isVisible = true
        holder.binding.tvPlatform.text = item.platform
        holder.binding.tvPlatform.setBackgroundResource(
            when (item.platform) {
                "epo" -> R.drawable.bg_scene_platform_epo
                "itwater" -> R.drawable.bg_scene_platform_itwater
                else -> R.drawable.bg_scene_platform_edc
            },
        )
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemSceneFunctionBinding) : RecyclerView.ViewHolder(binding.root)
}
