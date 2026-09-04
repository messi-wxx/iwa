package com.cq.iwa.sceneservice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivitySceneUpdateComponentBinding
import com.cq.iwa.databinding.ItemSceneComponentBinding
import com.cq.iwa.feature.sceneservice.network.ScenePartIdsDescDto
import com.cq.iwa.feature.sceneservice.ui.SceneUpdateComponentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SceneUpdateComponentActivity : IwaBaseActivity<ActivitySceneUpdateComponentBinding>() {

    private val viewModel: SceneUpdateComponentViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneUpdateComponentBinding =
        ActivitySceneUpdateComponentBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val id = intent.getStringExtra(SceneServiceNavigator.EXTRA_PRODUCT_ID).orEmpty()
        viewModel.onOpenRegister = { define, codes, productId ->
            SceneServiceNavigator.openRegister(this, define, codes, productId)
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnNext.setOnClickListener { viewModel.next() }
        binding.btnAdd.setOnClickListener { SceneServiceNavigator.openInputComponent(this) }
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                bindParts(ui.oldParts, ui.newParts)
            }
        }
        viewModel.load(id)
    }

    private fun bindParts(oldParts: List<ScenePartIdsDescDto>, newParts: List<ScenePartIdsDescDto>) {
        binding.oldContainer.removeAllViews()
        oldParts.forEach { part ->
            val item = ItemSceneComponentBinding.inflate(LayoutInflater.from(this), binding.oldContainer, false)
            item.tvName.text = part.partDefineInfoName.orEmpty()
            item.tvCode.text = listOf(part.partDefineInfoCode, part.code).filter { !it.isNullOrBlank() }.joinToString(" / ")
            item.btnAction.text = "添加"
            item.btnAction.setOnClickListener { viewModel.addOldPart(part) }
            binding.oldContainer.addView(item.root)
        }
        binding.newContainer.removeAllViews()
        newParts.forEach { part ->
            val item = ItemSceneComponentBinding.inflate(LayoutInflater.from(this), binding.newContainer, false)
            item.tvName.text = part.partDefineInfoName.orEmpty()
            item.tvCode.text = listOf(part.partDefineInfoCode, part.code).filter { !it.isNullOrBlank() }.joinToString(" / ")
            item.btnAction.text = "移除"
            item.btnAction.setOnClickListener { viewModel.removeNewPart(part) }
            binding.newContainer.addView(item.root)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SceneServiceNavigator.REQUEST_COMPONENT &&
            resultCode == SceneServiceNavigator.RESULT_COMPONENT
        ) {
            SceneServiceNavigator.part(data)?.let(viewModel::addNewPart)
        } else if (requestCode == SceneServiceNavigator.REQUEST_REGISTER &&
            resultCode == SceneServiceNavigator.RESULT_REGISTER
        ) {
            finish()
        }
    }
}
