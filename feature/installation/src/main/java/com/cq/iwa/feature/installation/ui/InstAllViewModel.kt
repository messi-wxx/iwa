package com.cq.iwa.feature.installation.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.installation.data.InstRepository
import com.cq.iwa.feature.installation.network.InstListRequest
import com.cq.iwa.feature.installation.network.InstProjectDto
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class InstAllUi(
    val items: List<InstProjectDto> = emptyList(),
    val empty: Boolean = false,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val hasUrge: Boolean = false,
    val version: Int = 0,
)

@HiltViewModel
class InstAllViewModel @Inject constructor(
    private val repository: InstRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var code: String? = null
    var address: String? = null
    var applicantInfo: String? = null
    var type: List<Int> = emptyList()
    var state: List<Int> = emptyList()
    var beginTime: String? = null
    var endTime: String? = null

    private val pageSize = 20
    private var page = 1

    private val _ui = MutableStateFlow(InstAllUi())
    val ui: StateFlow<InstAllUi> = _ui.asStateFlow()
    private var loadJob: Job? = null

    init {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val end = Calendar.getInstance()
        endTime = "${sdf.format(end.time)}T23:59:59"
        end.add(Calendar.DAY_OF_MONTH, -7)
        beginTime = "${sdf.format(end.time)}T00:00:00"
        viewModelScope.launch {
            val user = withContext(ioDispatcher) { authRepository.getCurrentUser() }
            val menus = authRepository.decodeMenus(user?.menuJson)
            val hasUrge = menus.any { menu ->
                menu.path == "installation" && menu.children.orEmpty().any { it.path == "urge" }
            }
            _ui.value = _ui.value.copy(hasUrge = hasUrge)
        }
        refresh()
    }

    fun refresh(overlay: Boolean = true) {
        page = 1
        load(false, overlay)
    }

    fun loadMore() {
        if (!_ui.value.hasMore) return
        page++
        load(true, overlay = false)
    }

    private fun load(more: Boolean, overlay: Boolean = !more) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (overlay) showLoading()
            try {
                val result = withContext(ioDispatcher) {
                    repository.getAllProjectList(
                        pageSize = pageSize,
                        currentPage = page,
                        body = InstListRequest(
                            code = code,
                            address = address,
                            applicantInfo = applicantInfo,
                            type = type,
                            state = state,
                            beginTime = beginTime,
                            endTime = endTime,
                        ),
                    )
                }
                when (result) {
                    is ApiResult.Error -> {
                        if (more) page--
                        showToast(result.message)
                        _ui.value = _ui.value.copy(version = _ui.value.version + 1)
                    }
                    is ApiResult.Success -> {
                        val pageItems = result.data.value
                        val items = if (more) _ui.value.items + pageItems else pageItems
                        _ui.value = _ui.value.copy(
                            items = items,
                            empty = items.isEmpty(),
                            total = result.data.key.pageTotal,
                            hasMore = pageItems.size >= pageSize && items.size < result.data.key.pageTotal,
                            version = _ui.value.version + 1,
                        )
                    }
                }
            } finally {
                hideLoading()
            }
        }
    }

    fun applyFilters(
        code: String?,
        address: String?,
        applicantInfo: String?,
        type: List<Int>,
        state: List<Int>,
        beginMillis: Long?,
        endMillis: Long?,
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        this.code = code
        this.address = address
        this.applicantInfo = applicantInfo
        this.type = type
        this.state = state
        this.beginTime = beginMillis?.let { "${sdf.format(Date(it))}T00:00:00" }
        this.endTime = endMillis?.let { "${sdf.format(Date(it))}T23:59:59" }
        refresh()
    }

    fun toggleFollow(projectId: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.followProject(projectId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("操作成功")
                    refresh()
                }
            }
        }
    }

    fun urge(projectId: Int, content: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.urgeProject(projectId, content) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> showToast("催办成功")
            }
        }
    }
}
