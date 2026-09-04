package com.cq.iwa.installation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.R
import com.cq.iwa.databinding.ItemInstKvBinding
import com.cq.iwa.databinding.ItemInstLogBinding
import com.cq.iwa.databinding.ItemInstMeterBinding
import com.cq.iwa.databinding.ItemInstProjectBinding
import com.cq.iwa.databinding.ItemInstTableBinding
import com.cq.iwa.feature.installation.data.InstFormat
import com.cq.iwa.feature.installation.network.InstLogDto
import com.cq.iwa.feature.installation.network.InstMeterRecordDto
import com.cq.iwa.feature.installation.network.InstProjectDto
import com.cq.iwa.feature.installation.network.InstSketchDto
import com.cq.iwa.feature.installation.network.InstTableDto

class InstProjectAdapter(
    private val onItem: (InstProjectDto) -> Unit,
    private val onFollow: ((InstProjectDto) -> Unit)? = null,
    private val onUrge: ((InstProjectDto) -> Unit)? = null,
) : RecyclerView.Adapter<InstProjectAdapter.Holder>() {
    private var items: List<InstProjectDto> = emptyList()
    var showFollow: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var showUrge: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submit(value: List<InstProjectDto>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvCode.text = "编号: ${item.code}"
        holder.binding.tvTask.text = item.taskName.orEmpty()
        holder.binding.tvName.text = "申请人: ${item.name}"
        holder.binding.tvAddress.text = "地址: ${item.address}"
        holder.binding.tvTime.text = "申请时间: ${item.formattedCreateTime}"
        holder.binding.tvState.text = "项目状态: ${item.stateDesc ?: "未知"}"
        holder.binding.tvFollow.isVisible = showFollow
        holder.binding.tvFollow.text = if (item.isFollow) "取消关注" else "关注"
        holder.binding.tvUrge.isVisible = showUrge && item.state != 3
        holder.binding.root.setOnClickListener { onItem(item) }
        holder.binding.tvFollow.setOnClickListener { onFollow?.invoke(item) }
        holder.binding.tvUrge.setOnClickListener { onUrge?.invoke(item) }
    }

    class Holder(val binding: ItemInstProjectBinding) : RecyclerView.ViewHolder(binding.root)
}

class InstSketchAdapter : RecyclerView.Adapter<InstSketchAdapter.Holder>() {
    private var items: List<InstSketchDto> = emptyList()
    fun submit(value: List<InstSketchDto>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstKvBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = item.label.orEmpty()
        holder.binding.tvValue.text = item.value.orEmpty().replace("T", " ")
    }

    class Holder(val binding: ItemInstKvBinding) : RecyclerView.ViewHolder(binding.root)
}

class InstTableAdapter(
    private val onClick: (InstTableDto) -> Unit,
) : RecyclerView.Adapter<InstTableAdapter.Holder>() {
    private var items: List<InstTableDto> = emptyList()
    fun submit(value: List<InstTableDto>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstTableBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.tableName.orEmpty()
        holder.binding.tvDetail.text = holder.itemView.context.getString(R.string.inst_table_count, item.datas.size)
        holder.binding.root.setOnClickListener { onClick(item) }
        holder.binding.tvDetail.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemInstTableBinding) : RecyclerView.ViewHolder(binding.root)
}

class InstMeterAdapter(
    private val onItem: (InstMeterRecordDto) -> Unit,
) : RecyclerView.Adapter<InstMeterAdapter.Holder>() {
    private var items: List<InstMeterRecordDto> = emptyList()
    fun submit(value: List<InstMeterRecordDto>) {
        items = value
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): InstMeterRecordDto? = items.getOrNull(position)

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstMeterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvMeterNo.text = "仪表编号: ${item.meterNo.ifBlank { "-" }}"
        holder.binding.tvUserNo.text = "用户编号: ${item.userNo.ifBlank { "-" }}"
        holder.binding.tvAddress.text = "地址: ${item.address.ifBlank { "-" }}"
        holder.binding.tvInit.text = "初始读数: ${item.initWater}  |  口径: ${item.caliber.ifBlank { "-" }}"
        holder.binding.tvType.text =
            "类型: ${item.type.ifBlank { "-" }}  |  供应商: ${item.factory.ifBlank { "-" }}"
        holder.itemView.setOnClickListener { onItem(item) }
    }

    class Holder(val binding: ItemInstMeterBinding) : RecyclerView.ViewHolder(binding.root)
}

class InstLogAdapter : RecyclerView.Adapter<InstLogAdapter.Holder>() {
    private var items: List<InstLogDto> = emptyList()
    fun submit(value: List<InstLogDto>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvLogType.text = item.typeDesc.orEmpty()
        holder.binding.tvOperator.text = item.createByName.orEmpty()
        holder.binding.tvContent.text = item.content.orEmpty()
        holder.binding.tvCreateTime.text = InstFormat.displayTime(item.createTime)
    }

    class Holder(val binding: ItemInstLogBinding) : RecyclerView.ViewHolder(binding.root)
}

fun instMeterSummary(item: InstMeterRecordDto): String = buildString {
    appendLine("用户编号: ${item.userNo.ifBlank { "-" }}")
    appendLine("地址: ${item.address.ifBlank { "-" }}")
    appendLine("初始读数: ${item.initWater}  |  口径: ${item.caliber.ifBlank { "-" }}")
    append("类型: ${item.type.ifBlank { "-" }}  |  供应商: ${item.factory.ifBlank { "-" }}")
    if (item.direction.isNotBlank()) append("  |  方向: ${item.direction}")
}
