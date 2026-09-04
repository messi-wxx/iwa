package com.cq.iwa.core.media

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class CapturePhotoHelper(private val activity: ComponentActivity) {

    private var pendingUri: Uri? = null
    private var captureCallback: ((Uri?) -> Unit)? = null
    private var pickCallback: ((Uri?) -> Unit)? = null

    private val captureLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        captureCallback?.invoke(if (success) pendingUri else null)
        captureCallback = null
        pendingUri = null
    }

    private val pickLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        pickCallback?.invoke(uri)
        pickCallback = null
    }

    fun createPictureFile(dir: File = defaultPictureDir()): File {
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "meter_${UUID.randomUUID()}.jpg")
    }

    private fun defaultPictureDir(): File {
        return activity.getExternalFilesDir("pictures") ?: File(activity.filesDir, "pictures")
    }

    suspend fun capture(outputFile: File): Uri? = suspendCancellableCoroutine { cont ->
        captureCallback = { uri ->
            if (cont.isActive) cont.resume(uri)
        }
        cont.invokeOnCancellation { captureCallback = null }
        if (!outputFile.exists()) {
            outputFile.parentFile?.mkdirs()
            outputFile.createNewFile()
        }
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            outputFile,
        )
        pendingUri = uri
        captureLauncher.launch(uri)
    }

    suspend fun pickImage(): Uri? = suspendCancellableCoroutine { cont ->
        pickCallback = { uri ->
            if (cont.isActive) cont.resume(uri)
        }
        cont.invokeOnCancellation { pickCallback = null }
        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun copyUriToFile(uri: Uri, dest: File): Boolean {
        return runCatching {
            dest.parentFile?.mkdirs()
            activity.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } != null && dest.exists() && dest.length() > 0L
        }.getOrDefault(false)
    }
}
