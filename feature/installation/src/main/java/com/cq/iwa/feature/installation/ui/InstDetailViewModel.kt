package com.cq.iwa.feature.installation.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.installation.data.InstRepository
import com.cq.iwa.feature.installation.network.InstChildNodeDto
import com.cq.iwa.feature.installation.network.InstOverviewDto
import com.cq.iwa.feature.installation.network.InstProcessDetailDto
import com.cq.iwa.feature.installation.network.InstProcessFormDto
import com.cq.iwa.feature.installation.network.InstSketchResultDto
import com.cq.iwa.feature.installation.network.InstTaskDetailDto
import com.cq.iwa.feature.installation.network.InstTimelineItem
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstDetailUi(
    val sketch: InstSketchResultDto? = null,
    val process: InstProcessDetailDto? = null,
    val timeline: List<InstTimelineItem> = emptyList(),
    val currentName: String = "",
    val taskDetail: InstTaskDetailDto? = null,
    val fabText: String = "去办理",
    val overview: InstOverviewDto? = null,
)

@HiltViewModel
class InstDetailViewModel @Inject constructor(
    private val repository: InstRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(InstDetailUi())
    val ui: StateFlow<InstDetailUi> = _ui.asStateFlow()

    fun load(projectId: Int, taskId: String) {
        viewModelScope.launch {
            showLoading()
            val sketch = withContext(ioDispatcher) { repository.getProjectSketch(projectId) }
            val process = withContext(ioDispatcher) { repository.getProcessInstanceDetail(projectId) }
            hideLoading()
            if (sketch is ApiResult.Error) showToast(sketch.message)
            if (process is ApiResult.Error) showToast(process.message)
            val processData = (process as? ApiResult.Success)?.data
            _ui.value = _ui.value.copy(
                sketch = (sketch as? ApiResult.Success)?.data,
                process = processData,
                timeline = buildTimeline(processData),
                currentName = processData?.activeProcNodeList?.firstOrNull()?.nodeDefName.orEmpty(),
            )
            val resolvedTaskId = taskId.ifBlank {
                processData?.activeProcNodeList?.firstOrNull()?.childNodeList?.firstOrNull()?.insId.orEmpty()
            }
            if (resolvedTaskId.isNotBlank()) loadTask(resolvedTaskId)
        }
    }

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getTaskDetail(taskId) }) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    val assignee = result.data.detail?.assignee
                    val claimMode = result.data.config?.claimMode
                    _ui.value = _ui.value.copy(
                        taskDetail = result.data,
                        fabText = if (assignee.isNullOrEmpty() && claimMode == 1) "去认领" else "去办理",
                    )
                }
            }
        }
    }

    fun claim(taskId: String, projectId: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.claimTask(taskId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("认领成功")
                    load(projectId, taskId)
                }
            }
        }
    }

    fun loadOverview(projectId: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getProcessOverview(projectId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    if (result.data.xml.isNullOrBlank()) {
                        showToast("未获取到流程数据")
                    } else {
                        _ui.value = _ui.value.copy(overview = result.data)
                    }
                }
            }
        }
    }

    fun consumeOverview() {
        _ui.value = _ui.value.copy(overview = null)
    }

    fun formOf(taskId: String): InstProcessFormDto? {
        val list = _ui.value.process?.processFormList.orEmpty()
        return if (taskId.equals("start", true)) {
            list.firstOrNull { it.type.equals("start", true) }
        } else {
            list.firstOrNull { it.taskId == taskId }
        }
    }

    private fun buildTimeline(detail: InstProcessDetailDto?): List<InstTimelineItem> {
        if (detail == null) return emptyList()
        val items = mutableListOf<InstTimelineItem>()
        detail.historyProcNodeList.filter { it.type == "END_EVENT" }.forEach { node ->
            items.add(InstTimelineItem.EndEventNode(node.nodeDefName, node.createTime))
        }
        detail.activeProcNodeList.firstOrNull()?.let { active ->
            items.add(InstTimelineItem.ActiveNode(active.nodeDefName, active.childNodeList))
        }
        detail.historyProcNodeGroupList.forEach { group ->
            items.add(InstTimelineItem.HistoryGroupNode(group.nodeDefName, group.groupStatus, group.childNodeList))
        }
        detail.historyProcNodeList.filter { it.type == "START_EVENT" }.forEach { node ->
            items.add(
                InstTimelineItem.HistoryNode(
                    nodeDefName = node.nodeDefName,
                    type = node.type,
                    childNode = InstChildNodeDto(
                        insId = node.insId,
                        type = node.commentList.firstOrNull()?.type.orEmpty(),
                        createTime = node.createTime,
                        endTime = node.endTime,
                        duration = node.duration,
                        assignName = detail.startUserName,
                        candidateGroupsName = node.candidateGroupsName,
                        candidateUsersName = node.candidateUsersName,
                        dueDate = node.dueDate,
                        isOverdue = node.isOverdue,
                        commentList = node.commentList,
                    ),
                ),
            )
        }
        return items
    }
}
