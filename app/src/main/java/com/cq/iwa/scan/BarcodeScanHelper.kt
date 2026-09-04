package com.cq.iwa.scan

import android.Manifest
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.R
import com.cq.iwa.core.permission.PermissionRequester
import com.cq.iwa.core.ui.toast.ToastUtils
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/**
 * 必须在 Activity.onCreate（STARTED 之前）构造，才能安全注册 Activity Result。
 */
class BarcodeScanHelper(
    private val activity: ComponentActivity,
    private val permissionRequester: PermissionRequester,
) {
    private var onResult: ((String) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            ToastUtils.show(
                activity,
                activity.getString(R.string.scene_scan_cancel),
                Toast.LENGTH_LONG,
            )
        } else {
            onResult?.invoke(contents)
        }
        onResult = null
    }

    fun scan(onCode: (String) -> Unit) {
        activity.lifecycleScope.launch {
            if (!permissionRequester.request(Manifest.permission.CAMERA)) {
                ToastUtils.show(activity, activity.getString(R.string.scene_camera_denied))
                return@launch
            }
            onResult = onCode
            launcher.launch(
                ScanOptions()
                    .setCaptureActivity(QrScanActivity::class.java)
                    .setPrompt(activity.getString(R.string.scene_scan_prompt))
                    .setCameraId(0)
                    .setBeepEnabled(false)
                    .setBarcodeImageEnabled(false),
            )
        }
    }
}
