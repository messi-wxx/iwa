package com.cq.iwa.core.permission

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 必须在 Activity.onCreate（STARTED 之前）构造，才能安全注册 Activity Result。
 */
class PermissionRequester(activity: ComponentActivity) {

    private var callback: ((List<String>) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val denied = result.filterValues { !it }.keys.toList()
        callback?.invoke(denied)
        callback = null
    }

    suspend fun request(vararg permissions: String): List<String> =
        suspendCancellableCoroutine { cont ->
            callback = { denied ->
                if (cont.isActive) cont.resume(denied)
            }
            cont.invokeOnCancellation { callback = null }
            launcher.launch(arrayOf(*permissions))
        }

    suspend fun request(permission: String): Boolean =
        request(*arrayOf(permission)).isEmpty()
}
