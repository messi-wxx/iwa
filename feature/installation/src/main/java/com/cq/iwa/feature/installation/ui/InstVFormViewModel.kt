package com.cq.iwa.feature.installation.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.installation.data.InstFormat
import com.cq.iwa.feature.installation.data.InstRepository
import com.cq.iwa.feature.installation.network.InstRecordInfoBody
import com.cq.iwa.feature.installation.network.InstRejectBody
import com.cq.iwa.feature.installation.network.InstRejectTargetDto
import com.cq.iwa.core.network.BuildConfig
import com.cq.iwa.feature.installation.network.InstExtendBody
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstVFormUi(
    val qtyText: String = "仪表数量",
    val rejectTargets: List<InstRejectTargetDto> = emptyList(),
    val finished: Boolean = false,
)

@HiltViewModel
class InstVFormViewModel @Inject constructor(
    private val repository: InstRepository,
    private val sessionStore: SessionStore,
    @ApplicationContext private val context: Context,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _ui = MutableStateFlow(InstVFormUi())
    val ui: StateFlow<InstVFormUi> = _ui.asStateFlow()

    private var planQty = 0
    private var uploadQty = 0

    fun loadMeterQty(projectId: Int) {
        viewModelScope.launch {
            val install = withContext(ioDispatcher) { repository.getMeterInstallInfoList(projectId) }
            val records = withContext(ioDispatcher) {
                repository.getMeterRecordInfoList(InstRecordInfoBody(projectId = projectId))
            }
            planQty = (install as? ApiResult.Success)?.data?.value?.sumOf { it.number } ?: 0
            uploadQty = (records as? ApiResult.Success)?.data?.key?.pageTotal ?: 0
            _ui.value = _ui.value.copy(qtyText = "仪表数量 [$uploadQty/$planQty]")
        }
    }

    fun complete(taskId: String, formJson: String) {
        viewModelScope.launch {
            showLoading()
            val body = InstFormat.wrapCompleteJson(taskId, formJson)
            val result = withContext(ioDispatcher) { repository.completeTask(body) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("任务完成")
                    _ui.value = _ui.value.copy(finished = true)
                }
            }
        }
    }

    fun loadRejectTargets(taskId: String, onLoaded: (List<InstRejectTargetDto>) -> Unit) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.getRejectTargets(taskId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> onLoaded(result.data.filter { it.canReject })
            }
        }
    }

    fun reject(taskId: String, target: String, reason: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.rejectTask(InstRejectBody(reason, taskId, target))
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("驳回成功")
                    _ui.value = _ui.value.copy(finished = true)
                }
            }
        }
    }

    fun extend(taskId: String, dueDate: String, reason: String) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                repository.extendTask(InstExtendBody(dueDate, reason, taskId))
            }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("延期成功")
                    _ui.value = _ui.value.copy(finished = true)
                }
            }
        }
    }

    fun dictionary(code: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getDictionaryOption(code) }) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> onSuccess(result.data)
            }
        }
    }

    fun options(code: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getOptionList(code) }) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> onSuccess(result.data)
            }
        }
    }

    fun meterInstallJson(projectId: Int, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.getMeterInstallInfoList(projectId) }) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    planQty = result.data.value.sumOf { it.number }
                    _ui.value = _ui.value.copy(qtyText = "仪表数量 [$uploadQty/$planQty]")
                    onSuccess(com.cq.iwa.feature.installation.network.InstJson.encode(result.data))
                }
            }
        }
    }

    fun downloadExcelTemplate() {
        viewModelScope.launch {
            val token = sessionStore.getToken().orEmpty()
            val url = BuildConfig.BASE_URL.trimEnd('/') +
                "/imp/v1/Business/Workbench/BatchWaterMeterExcleDownGet"
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("重庆智慧水务")
                setDescription("正在下载水表信息录入模板")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "水表信息录入模板.xlsx")
                if (token.isNotBlank()) {
                    addRequestHeader(
                        "Authorization",
                        if (token.startsWith("Bearer ")) token else "Bearer $token",
                    )
                }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            showToast("正在下载，请在通知栏查看进度")
        }
    }

    fun downloadFile(url: String, name: String) {
        viewModelScope.launch {
            val token = sessionStore.getToken().orEmpty()
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("重庆智慧水务")
                setDescription("正在下载附件")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                if (token.isNotBlank()) {
                    addRequestHeader(
                        "Authorization",
                        if (token.startsWith("Bearer ")) token else "Bearer $token",
                    )
                }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            showToast("正在下载，请在通知栏查看进度")
        }
    }

    fun downloadContract(projectId: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.downloadContract(projectId) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> showToast("下载成功，文件已保存到 Downloads")
            }
        }
    }

    fun downloadDocument(projectId: Int, type: Int) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.downloadDocument(projectId, type) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> showToast("下载成功，文件已保存到 Downloads")
            }
        }
    }

    fun consumeFinished() {
        _ui.value = _ui.value.copy(finished = false)
    }
}
