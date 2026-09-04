package com.cq.iwa.diagnose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDiagnoseTypeListBinding
import com.cq.iwa.databinding.ItemDiagnoseTypeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiagnoseTypeListActivity : IwaBaseActivity<ActivityDiagnoseTypeListBinding>() {

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDiagnoseTypeListBinding =
        ActivityDiagnoseTypeListBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { finish() }
        val items = listOf(
            getString(R.string.diagnose_nb_type),
            getString(R.string.diagnose_wired_title),
        )
        binding.rvTypes.layoutManager = LinearLayoutManager(this)
        binding.rvTypes.adapter = DiagnoseTypeAdapter(items) { index ->
            if (index == 0) {
                IwaDialogs.list(
                    this,
                    getString(R.string.diagnose_child_type),
                    listOf(
                        getString(R.string.diagnose_nb_common),
                        getString(R.string.diagnose_nb_dalian),
                    ),
                ) { which ->
                    if (which == 0) DiagnoseNavigator.openCommonNb(this)
                    else DiagnoseNavigator.openDalianNb(this)
                }
            } else {
                DiagnoseNavigator.openWired(this)
            }
        }
    }
}

private class DiagnoseTypeAdapter(
    private val items: List<String>,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<DiagnoseTypeAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDiagnoseTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.tvName.text = items[position]
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class Holder(val binding: ItemDiagnoseTypeBinding) : RecyclerView.ViewHolder(binding.root)
}
