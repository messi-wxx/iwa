package com.cq.iwa.feature.diagnose.ui

import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.diagnose.data.DiagnoseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DalianNbUi(
    val canWriteWater: Boolean = false,
)

@HiltViewModel
class DalianNbViewModel @Inject constructor(
    private val repository: DiagnoseRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(DalianNbUi())
    val ui: StateFlow<DalianNbUi> = _ui.asStateFlow()

    fun loadPermission() {
        launchApi({ repository.canDemoWriteWater() }) { result ->
            if (result is ApiResult.Success) {
                _ui.value = DalianNbUi(canWriteWater = result.data)
            }
        }
    }
}
