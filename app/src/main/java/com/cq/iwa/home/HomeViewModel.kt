package com.cq.iwa.home

import androidx.lifecycle.viewModelScope
import com.cq.iwa.R
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.network.VersionDto
import com.cq.iwa.update.AppUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeFunction(
    val title: String,
    val path: String,
    val iconRes: Int,
    val enabled: Boolean,
    val message: String = "",
)

data class HomeUiModel(
    val userName: String,
    val functions: List<HomeFunction>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appUpdateHelper: AppUpdateHelper,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<HomeUiModel>>(UiState.Idle)
    val uiState: StateFlow<UiState<HomeUiModel>> = _uiState.asStateFlow()

    private val _updateAvailable = MutableSharedFlow<VersionDto>(extraBufferCapacity = 1)
    val updateAvailable: SharedFlow<VersionDto> = _updateAvailable.asSharedFlow()

    fun loadHome() {
        launchUiState(_uiState) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("未登录")
            val menus = authRepository.decodeMenus(user.menuJson)
            val enabledPaths = menus.map { it.path }.toSet()

            val functions = listOf(
                template("抄表", "readMeterTask", R.drawable.ic_home_meter, enabledPaths),
                template("换表", "changeMeterTask", R.drawable.ic_home_replace, enabledPaths),
                template("催费", "urgePayment", R.drawable.ic_home_urge, enabledPaths),
                template("现场服务", "sceneService", R.drawable.ic_home_scene, enabledPaths),
                template("诊断", "diagnose", R.drawable.ic_home_diagnose, enabledPaths, aliases = setOf("MeterDiagnose")),
                template(
                    title = "水表标定",
                    path = "meterCalibration",
                    iconRes = R.drawable.ic_home_calibrate,
                    enabledPaths = enabledPaths,
                    aliases = setOf("meterLocation"),
                ),
                template("管网监测", "pipelineNetworkMonitoring", R.drawable.ic_home_dma, enabledPaths),
                template("报装工单", "installation", R.drawable.ic_home_scene, enabledPaths),
            )
            HomeUiModel(userName = user.name, functions = functions)
        }
    }

    fun checkApkVersion() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { appUpdateHelper.fetchLatest() }
            if (result is ApiResult.Success && appUpdateHelper.shouldPromptOnHome(result.data)) {
                _updateAvailable.emit(result.data)
            }
        }
    }

    private fun template(
        title: String,
        path: String,
        iconRes: Int,
        enabledPaths: Set<String>,
        aliases: Set<String> = emptySet(),
    ): HomeFunction {
        val enabled = path in enabledPaths || aliases.any { it in enabledPaths }
        return HomeFunction(
            title = title,
            path = path,
            iconRes = iconRes,
            enabled = enabled,
            message = if (enabled) "" else "暂无权限",
        )
    }
}
