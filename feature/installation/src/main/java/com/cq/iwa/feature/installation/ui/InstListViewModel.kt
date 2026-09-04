package com.cq.iwa.feature.installation.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class InstListUi(
    val items: List<InstProjectDto> = emptyList(),
    val empty: Boolean = false,
    val pendingCount: Int = 0,
    val urgeCount: Int = 0,
    val followCount: Int = 0,
    val workBenchCode: String = "pending",
    val listStamp: Int = 0,
)

@HiltViewModel
class InstListViewModel @Inject constructor(
    private val repository: InstRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var code: String? = null
    var address: String? = null
    var applicantInfo: String? = null
    var type: List<Int> = emptyList()
    var state: List<Int> = emptyList()
    var beginTime: String? = null
    var endTime: String? = null

    private val _ui = MutableStateFlow(InstListUi())
    val ui: StateFlow<InstListUi> = _ui.asStateFlow()
    private var loadJob: Job? = null

    fun load(workBenchCode: String, overlay: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (overlay) showLoading()
            try {
                val result = withContext(ioDispatcher) {
                    repository.getWorkbenchList(
                        pageSize = 1000,
                        currentPage = 1,
                        body = InstListRequest(
                            code = code,
                            address = address,
                            applicantInfo = applicantInfo,
                            type = type,
                            state = state,
                            beginTime = beginTime,
                            endTime = endTime,
                            workBenchCode = workBenchCode,
                        ),
                    )
                }
                val stamp = _ui.value.listStamp + 1
                when (result) {
                    is ApiResult.Error -> {
                        showToast(result.message)
                        _ui.value = _ui.value.copy(
                            items = emptyList(),
                            empty = true,
                            workBenchCode = workBenchCode,
                            listStamp = stamp,
                        )
                    }
                    is ApiResult.Success -> {
                        val items = result.data.value
                        _ui.value = _ui.value.copy(
                            items = items,
                            empty = items.isEmpty(),
                            workBenchCode = workBenchCode,
                            pendingCount = if (workBenchCode == "pending") items.size else _ui.value.pendingCount,
                            listStamp = stamp,
                        )
                    }
                }
                loadCounts()
            } finally {
                hideLoading()
            }
        }
    }

    fun loadCounts() {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getWorkbenchCount() }) {
                is ApiResult.Success -> {
                    _ui.value = _ui.value.copy(
                        pendingCount = result.data.title_Handler.firstOrNull()?.value ?: 0,
                        urgeCount = result.data.title_Initiator.firstOrNull()?.value ?: 0,
                        followCount = result.data.title_Completer.firstOrNull()?.value ?: 0,
                    )
                }
                is ApiResult.Error -> Unit
            }
        }
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
                    load(_ui.value.workBenchCode)
                }
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
        load(_ui.value.workBenchCode)
    }

    fun clearFilters() {
        code = null
        address = null
        applicantInfo = null
        type = emptyList()
        state = emptyList()
        beginTime = null
        endTime = null
    }
}
