package com.cq.iwa.core.media

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 必须在 Activity.onCreate（STARTED 之前）构造。
 */
class MediaPicker(activity: ComponentActivity) {

    private var callback: ((Uri?) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        callback?.invoke(uri)
        callback = null
    }

    suspend fun pickImage(): Uri? = suspendCancellableCoroutine { cont ->
        callback = { uri ->
            if (cont.isActive) cont.resume(uri)
        }
        cont.invokeOnCancellation { callback = null }
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
