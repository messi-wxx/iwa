package com.cq.iwa.feature.pipeline.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.pipeline.data.PipelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.cq.iwa.feature.pipeline.network.PipelineTreeItemDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PipelineTreeUi(
    val path: List<Int> = emptyList(),
    val current: List<PipelineTreeItemDto> = emptyList(),
    val title: String = "",
    val empty: Boolean = false,
)

@HiltViewModel
class PipelineTreeViewModel @Inject constructor(
    private val repository: PipelineRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private var roots: List<PipelineTreeItemDto> = emptyList()
    private val _ui = MutableStateFlow(PipelineTreeUi())
    val ui: StateFlow<PipelineTreeUi> = _ui.asStateFlow()

    fun load() {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getSiteTree() }
            hideLoading()
            when (result) {
                is ApiResult.Error -> {
                    showToast(result.message)
                    _ui.value = PipelineTreeUi(empty = true)
                }
                is ApiResult.Success -> {
                    roots = result.data
                    _ui.value = PipelineTreeUi(current = roots, empty = roots.isEmpty())
                }
            }
        }
    }

    fun enter(position: Int) {
        val node = currentNode()?.children?.getOrNull(position)
            ?: roots.getOrNull(position)
            ?: return
        if (node.children.isEmpty()) {
            showToast("暂无子节点")
            return
        }
        val path = _ui.value.path + position
        _ui.value = PipelineTreeUi(path = path, current = node.children, title = node.fullName)
    }

    fun back() {
        val path = _ui.value.path
        if (path.isEmpty()) return
        val next = path.dropLast(1)
        if (next.isEmpty()) {
            _ui.value = PipelineTreeUi(current = roots, empty = roots.isEmpty())
        } else {
            val node = nodeAt(next)
            _ui.value = PipelineTreeUi(
                path = next,
                current = node?.children.orEmpty(),
                title = node?.fullName.orEmpty(),
            )
        }
    }

    private fun currentNode(): PipelineTreeItemDto? = nodeAt(_ui.value.path)

    private fun nodeAt(path: List<Int>): PipelineTreeItemDto? {
        if (path.isEmpty()) return null
        var node = roots.getOrNull(path.first()) ?: return null
        path.drop(1).forEach { index ->
            node = node.children.getOrNull(index) ?: return null
        }
        return node
    }
}
