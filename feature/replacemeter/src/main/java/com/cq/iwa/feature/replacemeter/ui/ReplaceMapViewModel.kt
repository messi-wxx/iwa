package com.cq.iwa.feature.replacemeter.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ReplaceMapUi(
    val taskName: String,
    val unfinished: Int,
    val finished: Int,
    val notNeed: Int,
    val meters: List<ReplaceMeterEntity>,
)

@HiltViewModel
class ReplaceMapViewModel @Inject constructor(
    private val repository: ReplaceMeterRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<ReplaceMapUi>>(UiState.Idle)
    val uiState: StateFlow<UiState<ReplaceMapUi>> = _uiState.asStateFlow()

    fun load(taskId: String) {
        launchUiState(_uiState) {
            val book = repository.queryBook(taskId)
            val meters = repository.queryVisibleMeters(taskId)
            val (unfinished, finished, notNeed) = repository.mapCounts(taskId)
            ReplaceMapUi(
                taskName = book?.taskName.orEmpty(),
                unfinished = unfinished,
                finished = finished,
                notNeed = notNeed,
                meters = meters,
            )
        }
    }
}
