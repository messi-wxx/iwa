package com.cq.iwa.feature.replacemeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.replacemeter.ReplaceProgress
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReplaceMeterListViewModel @Inject constructor(
    private val repository: ReplaceMeterRepository,
    private val appSettings: AppSettings,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<ReplaceMeterListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<ReplaceMeterListUi>> = _uiState.asStateFlow()

    private var taskId: String = ""
    private var filter: ReplaceMeterFilter = ReplaceMeterFilter.UNFINISHED
    private var taskName: String = ""
    private var groups: List<String> = emptyList()
    private var loadingMore = false
    private var fetchJob: Job? = null
    private val pageSize = 20

    fun load(taskId: String, overlay: Boolean = true) {
        this.taskId = taskId
        fetch(append = false, overlay = overlay)
    }

    fun refresh() {
        fetch(append = false, overlay = false)
    }

    fun loadMore() {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        if (!data.hasMore || loadingMore) return
        fetch(append = true, overlay = false)
    }

    fun setFilter(next: ReplaceMeterFilter) {
        if (filter == next) return
        filter = next
        fetch(append = false, overlay = false)
    }

    fun setGroup(name: String) {
        appSettings.replaceGroupName = if (name == "所有分组") "" else name
        fetch(append = false, overlay = false)
    }

    fun search(keyword: String, onResult: (List<ReplaceMeterItemUi>) -> Unit) {
        viewModelScope.launch {
            val list = withContext(ioDispatcher) {
                repository.search(keyword, taskId).map { it.toItemUi() }
                    .filter { matchesGroup(it) }
                    .filter { matchesFilter(it) }
            }
            onResult(list)
        }
    }

    private fun fetch(append: Boolean, overlay: Boolean) {
        if (taskId.isBlank()) return
        if (append) loadingMore = true
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (overlay) _uiState.value = UiState.Loading
            try {
                val pageResult = withContext(ioDispatcher) {
                    val book = repository.queryBook(taskId)
                    taskName = book?.taskName.orEmpty()
                    groups = repository.queryGroups(taskId)
                    val groupName = appSettings.replaceGroupName
                    val (unfinished, finished) = repository.countsByGroup(taskId, groupName)
                    val offset = if (append) {
                        (_uiState.value as? UiState.Success)?.data?.meters?.size ?: 0
                    } else {
                        0
                    }
                    val page = repository.queryPagedMeters(
                        taskId = taskId,
                        groupName = groupName,
                        finished = filter == ReplaceMeterFilter.FINISHED,
                        pageSize = pageSize,
                        offset = offset,
                    ).map { it.toItemUi() }
                    PageResult(page, unfinished, finished, groupName)
                }
                val previous = if (append) {
                    (_uiState.value as? UiState.Success)?.data?.meters.orEmpty()
                } else {
                    emptyList()
                }
                val meters = previous + pageResult.page
                val previousTick = (_uiState.value as? UiState.Success)?.data?.tick ?: 0
                _uiState.value = UiState.Success(
                    ReplaceMeterListUi(
                        taskName = taskName,
                        meters = meters,
                        filter = filter,
                        groups = groups,
                        groupName = pageResult.groupName,
                        unfinished = pageResult.unfinished,
                        finished = pageResult.finished,
                        hasMore = pageResult.page.size >= pageSize,
                        tick = previousTick + 1,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "加载失败", error)
            } finally {
                if (fetchJob === coroutineContext[Job]) loadingMore = false
            }
        }
    }

    private fun matchesFilter(item: ReplaceMeterItemUi): Boolean = when (filter) {
        ReplaceMeterFilter.UNFINISHED -> item.progress < ReplaceProgress.BOTH
        ReplaceMeterFilter.FINISHED -> item.progress == ReplaceProgress.BOTH
    }

    private fun matchesGroup(item: ReplaceMeterItemUi): Boolean {
        val group = appSettings.replaceGroupName
        return group.isBlank() || item.extInfo == group
    }

    private data class PageResult(
        val page: List<ReplaceMeterItemUi>,
        val unfinished: Int,
        val finished: Int,
        val groupName: String,
    )
}

fun ReplaceMeterEntity.toItemUi() = ReplaceMeterItemUi(
    tableId = tableId,
    oldMeterCode = oldMeterCode.orEmpty(),
    address = address.orEmpty(),
    clientCode = clientCode.orEmpty(),
    oldReading = oldReading.orEmpty(),
    newMeterCode = newMeterCode.orEmpty(),
    newReading = newReading.orEmpty(),
    isReplace = isReplace,
    progress = progress,
    state = state,
    sort = sort,
    extInfo = extInfo.orEmpty(),
)
