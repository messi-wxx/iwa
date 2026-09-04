package com.cq.iwa.installation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.R
import com.cq.iwa.databinding.ItemInstAssigneeBinding
import com.cq.iwa.databinding.ItemInstCommentBinding
import com.cq.iwa.feature.installation.data.InstFormat
import com.cq.iwa.feature.installation.network.InstChildNodeDto
import com.cq.iwa.feature.installation.network.InstCommentDto
import com.cq.iwa.feature.installation.network.InstTimelineItem

class InstTimelineAdapter(
    private val onForm: (String, String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_END = 0
        private const val TYPE_ACTIVE = 1
        private const val TYPE_HISTORY_GROUP = 2
        private const val TYPE_HISTORY = 3
    }

    private var items: List<InstTimelineItem> = emptyList()
    private val expanded = mutableSetOf<Int>()

    fun submit(value: List<InstTimelineItem>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is InstTimelineItem.EndEventNode -> TYPE_END
        is InstTimelineItem.ActiveNode -> TYPE_ACTIVE
        is InstTimelineItem.HistoryGroupNode -> TYPE_HISTORY_GROUP
        is InstTimelineItem.HistoryNode -> TYPE_HISTORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_END -> EndHolder(inflater.inflate(R.layout.item_inst_timeline_end_event, parent, false))
            TYPE_ACTIVE -> ActiveHolder(inflater.inflate(R.layout.item_inst_timeline_active, parent, false))
            TYPE_HISTORY_GROUP -> HistoryGroupHolder(inflater.inflate(R.layout.item_inst_timeline_history_group, parent, false))
            else -> HistoryHolder(inflater.inflate(R.layout.item_inst_timeline_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is InstTimelineItem.EndEventNode -> (holder as EndHolder).bind(item)
            is InstTimelineItem.ActiveNode -> (holder as ActiveHolder).bind(item, position)
            is InstTimelineItem.HistoryGroupNode -> (holder as HistoryGroupHolder).bind(item, position)
            is InstTimelineItem.HistoryNode -> (holder as HistoryHolder).bind(item)
        }
    }

    private fun addChildren(container: LinearLayout, children: List<InstChildNodeDto>, history: Boolean) {
        container.removeAllViews()
        children.forEachIndexed { index, child ->
            if (index > 0) {
                val separator = View(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1,
                    ).apply {
                        val gap = (12 * container.context.resources.displayMetrics.density).toInt()
                        topMargin = gap
                        bottomMargin = gap
                    }
                    setBackgroundColor(Color.parseColor("#E5E7EB"))
                }
                container.addView(separator)
            }
            val row = ItemInstAssigneeBinding.inflate(LayoutInflater.from(container.context), container, false)
            row.tvAssignee.text = child.assignName.orEmpty()
            row.candidateBox.isVisible = !history
            row.candidateGroupRow.isVisible = !history && !child.candidateGroupsName.isNullOrBlank()
            row.tvCandidateGroups.text = child.candidateGroupsName.orEmpty()
            row.candidateUserRow.isVisible = !history && !child.candidateUsersName.isNullOrBlank()
            row.tvCandidateUsers.text = child.candidateUsersName.orEmpty()
            row.tvCreateTime.text = InstFormat.displayTime(child.createTime).ifBlank { "-" }
            row.tvDueDate.text = InstFormat.displayTime(child.dueDate).ifBlank { "-" }
            row.tvDueDate.setTextColor(
                Color.parseColor(if (child.isOverdue) "#EF4444" else "#64748B"),
            )
            row.endTimeRow.isVisible = history
            row.tvEndTime.text = InstFormat.displayTime(child.endTime).ifBlank { "-" }
            row.durationRow.isVisible = history && !child.duration.isNullOrBlank()
            row.tvDuration.text = child.duration.orEmpty()
            if (child.commentList.isNotEmpty()) {
                row.commentRecyclerView.isVisible = true
                row.commentRecyclerView.layoutManager = LinearLayoutManager(container.context)
                row.commentRecyclerView.adapter = InstCommentAdapter().also { it.submit(child.commentList) }
            } else {
                row.commentRecyclerView.isVisible = false
            }
            container.addView(row.root)
        }
    }

    private inner class EndHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.nodeNameText)
        private val event: TextView = view.findViewById(R.id.endEventText)
        fun bind(item: InstTimelineItem.EndEventNode) {
            name.text = item.nodeDefName
            event.text = "${InstFormat.displayTime(item.createTime)} 结束流程"
        }
    }

    private inner class ActiveHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.nodeNameText)
        private val count: TextView = view.findViewById(R.id.assigneeCountText)
        private val overdue: TextView = view.findViewById(R.id.overdueTag)
        private val arrow: ImageView = view.findViewById(R.id.expandArrow)
        private val detail: View = view.findViewById(R.id.detailCard)
        private val header: LinearLayout = view.findViewById(R.id.headerLayout)
        private val children: LinearLayout = view.findViewById(R.id.childContainer)

        fun bind(item: InstTimelineItem.ActiveNode, position: Int) {
            name.text = item.nodeDefName
            count.text = if (item.childNodeList.size > 1) "${item.childNodeList.size}人办理" else ""
            overdue.bindOverdue(InstFormat.overdueTag(item.childNodeList))
            if (item.childNodeList.size == 1) expanded.add(position)
            render(item, position)
            header.setOnClickListener {
                if (position in expanded) expanded.remove(position) else expanded.add(position)
                render(item, position)
            }
        }

        private fun render(item: InstTimelineItem.ActiveNode, position: Int) {
            val open = position in expanded
            arrow.setImageResource(if (open) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            detail.isVisible = open
            if (open) addChildren(children, item.childNodeList, false)
        }
    }

    private inner class HistoryGroupHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.nodeNameText)
        private val status: TextView = view.findViewById(R.id.statusTag)
        private val overdue: TextView = view.findViewById(R.id.overdueTag)
        private val form: ImageView = view.findViewById(R.id.vformButton)
        private val count: TextView = view.findViewById(R.id.assigneeCountText)
        private val arrow: ImageView = view.findViewById(R.id.expandArrow)
        private val dot: View = view.findViewById(R.id.dotBackground)
        private val iconDone: ImageView = view.findViewById(R.id.dotIconCompleted)
        private val iconReject: ImageView = view.findViewById(R.id.dotIconRejected)
        private val iconStop: ImageView = view.findViewById(R.id.dotIconTerminated)
        private val summary: LinearLayout = view.findViewById(R.id.collapsedSummary)
        private val summaryType: TextView = view.findViewById(R.id.summaryType)
        private val summaryUser: TextView = view.findViewById(R.id.summaryUser)
        private val summaryTime: TextView = view.findViewById(R.id.summaryTime)
        private val summaryMessage: TextView = view.findViewById(R.id.summaryMessage)
        private val detail: View = view.findViewById(R.id.detailCard)
        private val header: LinearLayout = view.findViewById(R.id.headerLayout)
        private val children: LinearLayout = view.findViewById(R.id.childContainer)

        fun bind(item: InstTimelineItem.HistoryGroupNode, position: Int) {
            name.text = item.nodeDefName
            iconDone.isVisible = false
            iconReject.isVisible = false
            iconStop.isVisible = false
            status.isVisible = true
            when (item.groupStatus) {
                "completed" -> {
                    dot.setBackgroundResource(R.drawable.circle_green)
                    iconDone.isVisible = true
                    form.isVisible = true
                    status.text = "已完成"
                    status.setBackgroundResource(R.drawable.bg_badge_green_stroke)
                    status.setTextColor(Color.parseColor("#10B981"))
                }
                "rejected" -> {
                    dot.setBackgroundResource(R.drawable.circle_red)
                    iconReject.isVisible = true
                    form.isVisible = false
                    status.text = "已驳回"
                    status.setBackgroundResource(R.drawable.bg_badge_red_stroke)
                    status.setTextColor(Color.parseColor("#EF4444"))
                }
                "terminated" -> {
                    dot.setBackgroundResource(R.drawable.circle_gray)
                    iconStop.isVisible = true
                    form.isVisible = false
                    status.text = "已终止"
                    status.setBackgroundResource(R.drawable.bg_badge_gray_stroke)
                    status.setTextColor(Color.parseColor("#6B7280"))
                }
                else -> {
                    dot.setBackgroundResource(R.drawable.circle_gray)
                    form.isVisible = false
                    status.isVisible = false
                }
            }
            count.text = if (item.childNodeList.size > 1) "${item.childNodeList.size}人办理" else ""
            overdue.bindOverdue(InstFormat.overdueTag(item.childNodeList))
            render(item, position)
            header.setOnClickListener {
                if (position in expanded) expanded.remove(position) else expanded.add(position)
                render(item, position)
            }
            form.setOnClickListener {
                onForm(item.childNodeList.firstOrNull()?.insId.orEmpty(), item.nodeDefName)
            }
        }

        private fun render(item: InstTimelineItem.HistoryGroupNode, position: Int) {
            val open = position in expanded
            arrow.setImageResource(if (open) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            val comment = item.childNodeList.firstOrNull()?.commentList?.firstOrNull()
            summary.isVisible = !open && comment != null
            if (comment != null) {
                summaryType.text = comment.type
                summaryUser.text = comment.userName
                summaryTime.text = InstFormat.displayTime(comment.createTime)
                summaryMessage.text = comment.message.orEmpty()
            }
            detail.isVisible = open
            if (open) addChildren(children, item.childNodeList, true)
        }
    }

    private inner class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.nodeNameText)
        private val form: ImageView = view.findViewById(R.id.vformButton)
        private val startCard: LinearLayout = view.findViewById(R.id.startEventCard)
        private val startText: TextView = view.findViewById(R.id.startEventText)
        private val status: TextView = view.findViewById(R.id.statusTag)
        private val overdue: TextView = view.findViewById(R.id.overdueTag)
        private val arrow: ImageView = view.findViewById(R.id.expandArrow)
        private val summary: LinearLayout = view.findViewById(R.id.collapsedSummary)
        private val detail: View = view.findViewById(R.id.detailCard)
        private val header: LinearLayout = view.findViewById(R.id.headerLayout)
        private val dot: View = view.findViewById(R.id.dotBackground)
        private val check: ImageView = view.findViewById(R.id.dotIconCheck)
        private val warning: ImageView = view.findViewById(R.id.dotIconWarning)

        fun bind(item: InstTimelineItem.HistoryNode) {
            name.text = item.nodeDefName
            status.isVisible = false
            overdue.isVisible = false
            arrow.isVisible = false
            summary.isVisible = false
            detail.isVisible = false
            header.isClickable = false
            dot.setBackgroundResource(R.drawable.circle_green)
            check.isVisible = true
            warning.isVisible = false
            if (item.type == "START_EVENT") {
                startCard.isVisible = true
                form.isVisible = true
                startText.text =
                    "${item.childNode.assignName.orEmpty()} 在 ${InstFormat.displayTime(item.childNode.createTime)} 发起流程"
                form.setOnClickListener {
                    onForm(
                        if (item.type == "START_EVENT") "start" else item.childNode.insId.orEmpty(),
                        item.nodeDefName,
                    )
                }
            } else {
                startCard.isVisible = false
                form.isVisible = false
            }
        }
    }
}

private fun TextView.bindOverdue(tag: String?) {
    isVisible = tag != null
    text = tag.orEmpty()
}

class InstCommentAdapter : RecyclerView.Adapter<InstCommentAdapter.Holder>() {
    private var items: List<InstCommentDto> = emptyList()
    fun submit(value: List<InstCommentDto>) {
        items = value
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemInstCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvCommentUser.text = "${item.type} ${item.userName}".trim()
        holder.binding.tvCommentTime.text = InstFormat.displayTime(item.createTime)
        holder.binding.tvCommentMessage.isVisible = !item.message.isNullOrBlank()
        holder.binding.tvCommentMessage.text = item.message.orEmpty()
    }

    class Holder(val binding: ItemInstCommentBinding) : RecyclerView.ViewHolder(binding.root)
}
