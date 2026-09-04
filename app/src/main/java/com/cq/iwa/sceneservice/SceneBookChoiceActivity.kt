package com.cq.iwa.sceneservice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneListBinding
import com.cq.iwa.databinding.ItemSceneFunctionBinding
import com.cq.iwa.feature.sceneservice.network.SceneBookDto
import com.cq.iwa.feature.sceneservice.network.SceneJson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SceneBookChoiceActivity : IwaBaseActivity<ActivitySceneListBinding>() {

    private lateinit var root: List<SceneBookDto>
    private var current: List<SceneBookDto> = emptyList()
    private val path = mutableListOf<Int>()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneListBinding =
        ActivitySceneListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.tvTitle.text = getString(R.string.scene_book_title)
        binding.btnBack.setOnClickListener { goBack() }
        root = SceneServiceNavigator.books(intent)
        current = root
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = BookAdapter { onClick(it) }
        refresh()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goBack()
            },
        )
    }

    private fun refresh() {
        (binding.rvItems.adapter as BookAdapter).submit(current)
    }

    private fun onClick(position: Int) {
        val item = current.getOrNull(position) ?: return
        val children = item.children
        if (!children.isNullOrEmpty()) {
            path.add(position)
            current = children
            refresh()
        } else {
            setResult(
                RESULT_OK,
                Intent().putExtra(SceneServiceNavigator.EXTRA_BOOK, SceneJson.encode(item)),
            )
            finish()
        }
    }

    private fun goBack() {
        if (path.isEmpty()) {
            finish()
            return
        }
        path.removeAt(path.lastIndex)
        current = if (path.isEmpty()) {
            root
        } else {
            var list = root
            path.forEach { index -> list = list[index].children.orEmpty() }
            list
        }
        refresh()
    }
}

private class BookAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<BookAdapter.Holder>() {

    private var items: List<SceneBookDto> = emptyList()

    fun submit(list: List<SceneBookDto>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSceneFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.label.orEmpty()
        holder.binding.tvPlatform.isVisible = false
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemSceneFunctionBinding) : RecyclerView.ViewHolder(binding.root)
}
