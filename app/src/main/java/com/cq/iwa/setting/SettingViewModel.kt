package com.cq.iwa.setting

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.cq.iwa.R
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.common.model.UiState
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.auth.repository.AuthRepository
import com.cq.iwa.feature.readmeter.data.MeterLocalStore
import com.cq.iwa.feature.readmeter.data.MeterRepository
import com.cq.iwa.feature.readmeter.network.FileApi
import com.cq.iwa.feature.readmeter.network.VersionDto
import com.cq.iwa.feature.replacemeter.data.ReplaceMeterRepository
import com.cq.iwa.update.AppUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingUiModel(
    val userName: String = "",
    val userCode: String = "",
    val customerCode: String = "",
    val versionName: String = "",
    val autoNext: Boolean = true,
    val cacheSizeText: String = "0.0 MB",
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val meterRepository: MeterRepository,
    private val replaceMeterRepository: ReplaceMeterRepository,
    private val meterLocalStore: MeterLocalStore,
    private val fileApi: FileApi,
    private val appUpdateHelper: AppUpdateHelper,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _uiState = MutableStateFlow<UiState<SettingUiModel>>(UiState.Idle)
    val uiState: StateFlow<UiState<SettingUiModel>> = _uiState.asStateFlow()

    private val _updateAvailable = MutableSharedFlow<VersionDto>(extraBufferCapacity = 1)
    val updateAvailable: SharedFlow<VersionDto> = _updateAvailable.asSharedFlow()

    fun load(context: Context) {
        launchUiState(_uiState) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("未找到当前用户")
            SettingUiModel(
                userName = user.name,
                userCode = user.code,
                customerCode = user.customerCode,
                versionName = readVersionName(context),
                autoNext = authRepository.isAutoNextEnabled(),
                cacheSizeText = formatSize(meterLocalStore.accountPictureSize(user.customerCode, user.code)),
            )
        }
    }

    fun saveAutoNext(enabled: Boolean) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                authRepository.setAutoNextEnabled(enabled)
            }
            _uiState.update { state ->
                if (state is UiState.Success) {
                    UiState.Success(state.data.copy(autoNext = enabled))
                } else {
                    state
                }
            }
            showToast(R.string.settings_modify_success)
        }
    }

    suspend fun clearMeterCache(): String = withContext(ioDispatcher) {
        val user = authRepository.getCurrentUser() ?: error("未找到当前用户")
        meterRepository.clearCurrentAccount()
        replaceMeterRepository.clearCurrentAccount()
        meterLocalStore.clearAccountPictures(user.customerCode, user.code)
        formatSize(0)
    }

    suspend fun logoutAndClear() {
        withContext(ioDispatcher) {
            authRepository.logout()
        }
    }

    fun uploadBackup(context: Context) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) {
                val dbFile = File(context.applicationContext.getDatabasePath("iwa.db").absolutePath)
                if (!dbFile.exists()) {
                    return@withContext BackupUploadResult.Empty
                }
                val user = authRepository.getCurrentUser()
                    ?: return@withContext BackupUploadResult.Fail
                val stamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
                val fileName = "${user.customerCode}_${user.name}_${stamp}_${dbFile.name}"
                val body = MultipartBody.Part.createFormData(
                    "file",
                    fileName,
                    dbFile.asRequestBody("multipart/form-data".toMediaTypeOrNull()),
                )
                when (ApiExceptionHandler.safeApiCall { fileApi.uploadFile(body) }) {
                    is ApiResult.Success -> BackupUploadResult.Success
                    is ApiResult.Error -> BackupUploadResult.Fail
                }
            }
            hideLoading()
            when (result) {
                BackupUploadResult.Success -> showToast(R.string.settings_backup_success)
                BackupUploadResult.Empty -> showToast(R.string.settings_backup_empty)
                BackupUploadResult.Fail -> showToast(R.string.settings_backup_fail)
            }
        }
    }

    fun checkApkVersion() {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { appUpdateHelper.fetchLatest() }
            hideLoading()
            when (result) {
                is ApiResult.Success -> {
                    if (appUpdateHelper.hasNewerVersion(result.data)) {
                        _updateAvailable.emit(result.data)
                    } else {
                        showToast(R.string.settings_already_latest)
                    }
                }
                is ApiResult.Error -> showToast(result.message)
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / 1_000_000.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    private fun readVersionName(context: Context): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    private enum class BackupUploadResult {
        Success,
        Empty,
        Fail,
    }
}
