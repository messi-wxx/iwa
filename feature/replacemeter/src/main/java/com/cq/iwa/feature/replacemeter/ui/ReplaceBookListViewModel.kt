package com.cq.iwa.feature.replacemeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.sync.SyncProgress
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import com.cq.iwa.feature.replacemeter.sync.ReplaceMeterSyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

@HiltViewModel
class ReplaceBookListViewModel @Inject constructor(
    private val repository: ReplaceMeterRepository,
    private val syncEngine: ReplaceMeterSyncEngine,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<ReplaceBookListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<ReplaceBookListUi>> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    private var current = ReplaceBookListUi(
        platform = MeterPlatform.EDC.key,
        isEdc = true,
        selecting = false,
        books = emptyList(),
    )

    fun load(selectMode: Boolean? = null) {
        launchUiState(_uiState, isEmpty = { it.books.isEmpty() }) {
            val platform = repository.platform()
            val books = repository.queryBooks().map { it.toUi() }
            current = current.copy(
                platform = platform.key,
                isEdc = platform == MeterPlatform.EDC,
                selecting = when {
                    books.isEmpty() -> false
                    selectMode != null -> selectMode
                    else -> current.selecting
                },
                books = books,
                tick = current.tick + 1,
            )
            current
        }
    }

    fun toggleSelectMode(enabled: Boolean) {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        current = data.copy(
            selecting = enabled,
            books = data.books.map { it.copy(selected = if (enabled) it.selected else false) },
        )
        _uiState.value = UiState.Success(current)
    }

    fun toggleBook(taskId: String) {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        current = data.copy(
            books = data.books.map {
                if (it.taskId == taskId) it.copy(selected = !it.selected) else it
            },
        )
        _uiState.value = UiState.Success(current)
    }

    fun toggleAll() {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        val allSelected = data.books.all { it.selected }
        current = data.copy(books = data.books.map { it.copy(selected = !allSelected) })
        _uiState.value = UiState.Success(current)
    }

    fun syncSelected() {
        val data = (_uiState.value as? UiState.Success)?.data
        if (data?.isEdc == false) {
            showToast("换表任务仅支持 EDC 平台")
            return
        }
        val ids = data?.books?.filter { it.selected }?.map { it.taskId }.orEmpty()
        if (ids.isEmpty()) {
            showToast("请选择任务后再同步")
            return
        }
        startSync(ids)
    }

    fun refreshCatalog() {
        if (current.isEdc.not() && (_uiState.value as? UiState.Success)?.data?.isEdc == false) {
            showToast("换表任务仅支持 EDC 平台")
            return
        }
        viewModelScope.launch {
            try {
                val platform = withContext(ioDispatcher) { repository.platform() }
                if (platform != MeterPlatform.EDC) {
                    showToast("换表任务仅支持 EDC 平台")
                    return@launch
                }
                val result = withContext(ioDispatcher) {
                    syncEngine.refreshCatalog { snapshot ->
                        _progress.value = snapshot
                    }
                }
                load(selectMode = result.finished)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitFailedOnce(e)
            }
        }
    }

    private fun startSync(taskIds: List<String>) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    syncEngine.sync(taskIds) { snapshot ->
                        _progress.value = snapshot
                    }
                }
                load(selectMode = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitFailedOnce(e)
            }
        }
    }

    private fun emitFailedOnce(e: Exception) {
        if (_progress.value.finished) return
        _progress.value = SyncProgress(
            title = "同步换表数据",
            running = false,
            finished = true,
            errors = listOf(ApiExceptionHandler.userMessage(e)),
        )
    }

    fun consumeProgress() {
        _progress.value = SyncProgress()
    }

    fun search(keyword: String, onResult: (List<ReplaceMeterItemUi>) -> Unit) {
        viewModelScope.launch {
            val list = withContext(ioDispatcher) {
                repository.search(keyword).map { it.toItemUi() }
            }
            onResult(list)
        }
    }

    private suspend fun MeterBookEntity.toUi(): ReplaceBookItemUi {
        val (total, unfinished, finished) = repository.bookCounts(taskId)
        return ReplaceBookItemUi(
            taskId = taskId,
            taskName = taskName,
            lastUpdateTime = lastUpdateTime,
            taskState = taskState,
            total = total,
            unfinished = unfinished,
            finished = finished,
            downloaded = downloadTime > 0 || total > 0,
        )
    }
}
