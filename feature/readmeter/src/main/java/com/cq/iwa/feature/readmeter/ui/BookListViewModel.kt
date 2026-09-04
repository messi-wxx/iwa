package com.cq.iwa.feature.readmeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.readmeter.MeterPlatform
import com.cq.iwa.feature.readmeter.data.MeterRepository
import com.cq.iwa.feature.readmeter.sync.MeterSyncEngine
import com.cq.iwa.feature.readmeter.sync.SyncProgress
import com.cq.iwa.feature.readmeter.sync.SyncRequest
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
class BookListViewModel @Inject constructor(
    private val repository: MeterRepository,
    private val syncEngine: MeterSyncEngine,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<BookListUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<BookListUi>> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    private var current = BookListUi(
        platform = MeterPlatform.EDC.key,
        isBcp = false,
        selecting = false,
        includeNetMeter = false,
        books = emptyList(),
    )

    fun load(selectMode: Boolean? = null) {
        launchUiState(_uiState, isEmpty = { it.books.isEmpty() }) {
            val platform = repository.platform()
            val books = repository.queryBooks().map { it.toUi() }
            current = current.copy(
                platform = platform.key,
                isBcp = platform == MeterPlatform.BCP,
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

    fun setIncludeNetMeter(checked: Boolean) {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        current = data.copy(includeNetMeter = checked)
        _uiState.value = UiState.Success(current)
    }

    fun syncSelected() {
        val data = (_uiState.value as? UiState.Success)?.data
        val ids = data?.books?.filter { it.selected }?.map { it.taskId }.orEmpty()
        if (ids.isEmpty()) {
            showToast("请选择表册后再同步")
            return
        }
        startSync(ids, data?.includeNetMeter ?: current.includeNetMeter)
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            try {
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

    private fun startSync(taskIds: List<String>, includeNetMeter: Boolean) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    syncEngine.sync(SyncRequest(taskIds, includeNetMeter)) { snapshot ->
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
            running = false,
            finished = true,
            errors = listOf(ApiExceptionHandler.userMessage(e)),
        )
    }

    fun consumeProgress() {
        _progress.value = SyncProgress()
    }

    fun search(keyword: String, onResult: (List<MeterItemUi>) -> Unit) {
        viewModelScope.launch {
            val list = withContext(ioDispatcher) {
                repository.search(keyword).map { meter ->
                    MeterItemUi(
                        tableId = meter.tableId,
                        meterCode = meter.meterCode.orEmpty(),
                        clientName = meter.clientName.orEmpty(),
                        address = meter.address.orEmpty(),
                        state = meter.state,
                        sort = meter.sort,
                        reading = meter.reading.orEmpty(),
                    )
                }
            }
            onResult(list)
        }
    }

    private suspend fun MeterBookEntity.toUi(): BookItemUi {
        val (total, unread, read) = repository.bookCounts(taskId)
        return BookItemUi(
            taskId = taskId,
            taskName = taskName,
            lastUpdateTime = lastUpdateTime,
            taskState = taskState,
            total = total,
            unread = unread,
            read = read,
            downloaded = downloadTime > 0 || total > 0,
        )
    }
}
