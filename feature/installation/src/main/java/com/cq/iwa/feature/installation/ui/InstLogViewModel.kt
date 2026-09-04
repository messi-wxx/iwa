package com.cq.iwa.feature.installation.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.installation.data.InstFormat
import com.cq.iwa.feature.installation.data.InstRepository
import com.cq.iwa.feature.installation.network.InstLogDto
import com.cq.iwa.feature.installation.network.InstLogRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstLogUi(
    val items: List<InstLogDto> = emptyList(),
    val empty: Boolean = false,
    val types: List<Pair<String, String>> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
)

@HiltViewModel
class InstLogViewModel @Inject constructor(
    private val repository: InstRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    var projectId: Int = 0
    private var type: List<Int> = emptyList()
    private val _ui = MutableStateFlow(InstLogUi())
    val ui: StateFlow<InstLogUi> = _ui.asStateFlow()

    fun setup(projectId: Int) {
        this.projectId = projectId
        val start = InstFormat.daysAgo(7)
        val end = InstFormat.now("yyyy-MM-dd")
        _ui.value = _ui.value.copy(startDate = start, endDate = end)
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getDictionaryOption("LogType") }) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(types = InstFormat.parseOptions(result.data))
                is ApiResult.Error -> Unit
            }
        }
        load()
    }

    fun setDates(start: String, end: String) {
        _ui.value = _ui.value.copy(startDate = start, endDate = end)
        load()
    }

    fun setTypes(ids: List<Int>) {
        type = ids
        load()
    }

    fun load() {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.getProjectLogs(
                    InstLogRequest(
                        beginTime = "${_ui.value.startDate}T00:00:00",
                        endTime = "${_ui.value.endDate}T23:59:59",
                        projectId = projectId,
                        type = type,
                    ),
                )
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    items = result.data.value,
                    empty = result.data.value.isEmpty(),
                )
            }
        }
    }
}
