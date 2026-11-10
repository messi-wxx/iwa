package com.cq.iwa.feature.dma.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.dma.data.DmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class DmaListViewModel @Inject constructor(
    private val dmaRepository: DmaRepository,
) : BaseViewModel() {

    private val pageSize = 10
    private var source: List<DmaPartitionUi> = emptyList()
    private var fetchJob: Job? = null
    private var optionJob: Job? = null
    private var loadingMore = false
    private var waitGeneration = 0
    private val _ui = MutableStateFlow(DmaListUi())
    val ui: StateFlow<DmaListUi> = _ui.asStateFlow()
    private val _options = MutableStateFlow(DmaFilterOptions())
    val options: StateFlow<DmaFilterOptions> = _options.asStateFlow()

    fun refresh() {
        fetch(append = false)
    }

    fun loadMore() {
        if (!_ui.value.hasMore || loadingMore) return
        fetch(append = true)
    }

    fun setKeyword(keyword: String) {
        if (keyword == _ui.value.keyword) return
        _ui.value = _ui.value.copy(keyword = keyword)
        refresh()
    }

    fun setStatus(status: DmaStatusFilter) {
        if (status == _ui.value.status) return
        _ui.value = _ui.value.copy(status = status)
        refresh()
    }

    fun setSort(sort: DmaSort) {
        if (sort == _ui.value.sort) return
        _ui.value = _ui.value.copy(sort = sort)
        refresh()
    }

    fun setScope(levelNum: Int?, levelName: String, parentId: Int?, parentName: String) {
        val current = _ui.value
        if (current.levelNum == levelNum && current.parentId == parentId) return
        _ui.value = current.copy(
            levelNum = levelNum,
            levelName = levelName,
            parentId = parentId,
            parentName = parentName,
        )
        refresh()
    }

    fun loadFilterOptions() {
        if (optionJob?.isActive == true || _options.value.ready) return
        optionJob = viewModelScope.launch {
            val levelsResult = withContext(ioDispatcher) { dmaRepository.levelOptions() }
            val parentsResult = withContext(ioDispatcher) { dmaRepository.parent() }
            val levels = (levelsResult as? ApiResult.Success)?.data
                .orEmpty()
                .mapNotNull { option ->
                    option.label.takeIf { it.isNotBlank() }?.let { DmaOption(option.value, it) }
                }
            val parents = (parentsResult as? ApiResult.Success)?.data
                .orEmpty()
                .mapNotNull { option ->
                    option.name.takeIf { it.isNotBlank() }?.let { DmaOption(option.id, it) }
                }
            val loaded = levelsResult is ApiResult.Success || parentsResult is ApiResult.Success
            _options.value = DmaFilterOptions(
                levels = levels,
                parents = parents,
                ready = loaded && (levels.isNotEmpty() || parents.isNotEmpty()),
            )
            if (levelsResult is ApiResult.Error && parentsResult is ApiResult.Error) {
                showToast(levelsResult.message.ifBlank { "筛选项加载失败" })
            }
        }
    }

    private fun fetch(append: Boolean) {
        if (append) loadingMore = true
        fetchJob?.cancel()
        val requestPage = if (append) loadedPage() + 1 else 1
        val current = _ui.value
        val keyword = current.keyword.trim().ifBlank { null }
        val state = when (current.status) {
            DmaStatusFilter.ALL -> null
            DmaStatusFilter.NORMAL -> 0
            DmaStatusFilter.WARN -> 1
            DmaStatusFilter.SEVERE -> 2
        }
        val levelNum = current.levelNum
        val parentId = current.parentId
        val sortKind = current.sort.kind
        val generation = if (append) waitGeneration else ++waitGeneration
        if (!append) showLoading()
        fetchJob = viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    dmaRepository.list(
                        filterText = keyword,
                        levelNum = levelNum,
                        parentId = parentId,
                        state = state,
                        sortKind = sortKind,
                        pageSize = pageSize,
                        currentPage = requestPage,
                    )
                }
                when (result) {
                    is ApiResult.Success -> applyPage(
                        page = parseDmaList(result.data),
                        append = append,
                        updateStat = keyword == null &&
                            state == null &&
                            levelNum == null &&
                            parentId == null &&
                            sortKind == null,
                    )
                    is ApiResult.Error -> {
                        showToast(result.message.ifBlank { "加载失败" })
                        bump()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.message ?: "加载失败")
                bump()
            } finally {
                if (fetchJob === coroutineContext[Job]) loadingMore = false
                if (!append && generation == waitGeneration) hideLoading()
            }
        }
    }

    private fun applyPage(page: DmaListPage, append: Boolean, updateStat: Boolean) {
        val items = if (append) source + page.items else page.items
        source = items
        val hasMore = items.size < page.pageTotal && page.items.size >= pageSize
        val current = _ui.value
        _ui.value = current.copy(
            total = if (updateStat) page.total else current.total,
            normal = if (updateStat) page.normal else current.normal,
            warn = if (updateStat) page.warn else current.warn,
            severe = if (updateStat) page.severe else current.severe,
            unknown = if (updateStat) page.unknown else current.unknown,
            partitions = items,
            hasMore = hasMore,
            loadTick = current.loadTick + 1,
        )
    }

    private fun bump() {
        _ui.value = _ui.value.copy(loadTick = _ui.value.loadTick + 1)
    }

    private fun loadedPage(): Int {
        if (source.isEmpty()) return 0
        return (source.size + pageSize - 1) / pageSize
    }
}
