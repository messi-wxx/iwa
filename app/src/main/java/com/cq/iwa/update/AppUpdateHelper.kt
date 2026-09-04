package com.cq.iwa.update

import android.app.Activity
import android.content.Context
import android.os.Build
import com.cq.iwa.R
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.network.ApiExceptionHandler
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.feature.readmeter.network.FileApi
import com.cq.iwa.feature.readmeter.network.VersionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateHelper @Inject constructor(
    private val fileApi: FileApi,
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context,
) {

    fun currentVersionCode(): Long {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrDefault(0L)
    }

    suspend fun fetchLatest(): ApiResult<VersionDto> {
        return ApiExceptionHandler.safeApiCall { fileApi.getVersionInfo("version2.json") }
    }

    fun hasNewerVersion(version: VersionDto): Boolean {
        return version.ver.toLong() > currentVersionCode()
    }

    fun shouldPromptOnHome(version: VersionDto): Boolean {
        return hasNewerVersion(version) && version.ver != appSettings.skippedUpdateVer
    }

    fun promptUpdate(
        activity: Activity,
        version: VersionDto,
        showDontRemind: Boolean = false,
    ) {
        val title = activity.getString(R.string.settings_update_title)
        val message = version.content.ifBlank {
            activity.getString(R.string.settings_update_message)
        }
        val confirmText = activity.getString(R.string.settings_update_now)
        val cancelText = activity.getString(R.string.settings_update_ignore)
        if (showDontRemind) {
            IwaDialogs.confirmWithCheck(
                context = activity,
                title = title,
                message = message,
                checkText = activity.getString(R.string.settings_update_dont_remind),
                confirmText = confirmText,
                cancelText = cancelText,
                cancelable = false,
                onConfirm = { AppUpdater.getInstance(activity).downloadApk(version.path) },
                onCancel = { checked ->
                    if (checked) appSettings.skippedUpdateVer = version.ver
                },
            )
        } else {
            IwaDialogs.confirm(
                context = activity,
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText,
                cancelable = false,
                onConfirm = { AppUpdater.getInstance(activity).downloadApk(version.path) },
            )
        }
    }
}
