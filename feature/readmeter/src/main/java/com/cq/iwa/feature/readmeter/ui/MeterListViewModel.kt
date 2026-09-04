package com.cq.iwa.feature.readmeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.readmeter.MeterState
import com.cq.iwa.feature.readmeter.data.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MeterListViewModel @Inject constructor(
    private val repository: MeterRepository,
    private val appSettings: AppSettings,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<MeterListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<MeterListUi>> = _uiState.asStateFlow()

    private var taskId: String = ""
    private var filter: MeterFilter = MeterFilter.ALL
    private var taskName: String = ""
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

    fun setFilter(next: MeterFilter) {
        if (filter == next) return
        filter = next
        fetch(append = false, overlay = false)
    }

    fun setGroup(name: String) {
        appSettings.readMeterGroupName = if (name == "所有分组") "" else name
        fetch(append = false, overlay = false)
    }

    fun deleteMeter(tableId: Long) {
        viewModelScope.launch {
            val ok = withContext(ioDispatcher) { repository.deleteReadMeter(tableId) }
            if (ok) {
                showToast("删除成功")
                fetch(append = false, overlay = false)
            } else {
                showToast("删除失败")
            }
        }
    }

    fun search(keyword: String, onResult: (List<MeterItemUi>) -> Unit) {
        viewModelScope.launch {
            val list = withContext(ioDispatcher) {
                val group = appSettings.readMeterGroupName
                repository.search(keyword, taskId)
                    .filter { group.isBlank() || it.groupName == group }
                    .filter { matchesFilter(it) }
                    .map { it.toItemUi() }
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
                    val groupName = appSettings.readMeterGroupName
                    val groups = repository.queryGroups(taskId)
                    val offset = if (append) {
                        (_uiState.value as? UiState.Success)?.data?.meters?.size ?: 0
                    } else {
                        0
                    }
                    val page = repository.queryPagedMeters(
                        taskId, filter, pageSize, offset, groupName,
                    ).map { it.toItemUi() }
                    PageResult(page, groups, groupName)
                }
                val previous = if (append) {
                    (_uiState.value as? UiState.Success)?.data?.meters.orEmpty()
                } else {
                    emptyList()
                }
                val meters = previous + pageResult.page
                val previousTick = (_uiState.value as? UiState.Success)?.data?.tick ?: 0
                _uiState.value = UiState.Success(
                    MeterListUi(
                        taskName = taskName,
                        meters = meters,
                        filter = filter,
                        groups = pageResult.groups,
                        groupName = pageResult.groupName,
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

    private fun matchesFilter(meter: ReadMeterEntity): Boolean = when (filter) {
        MeterFilter.ALL -> meter.state > MeterState.DELETED
        MeterFilter.UNREAD -> meter.state == MeterState.UNREAD
        MeterFilter.READ -> meter.state > MeterState.UNREAD
    }

    private data class PageResult(
        val page: List<MeterItemUi>,
        val groups: List<String>,
        val groupName: String,
    )
}

private fun ReadMeterEntity.toItemUi() = MeterItemUi(
    tableId = tableId,
    meterCode = meterCode.orEmpty(),
    clientName = clientName.orEmpty(),
    address = address.orEmpty(),
    state = state,
    sort = sort,
    reading = reading.orEmpty(),
)
