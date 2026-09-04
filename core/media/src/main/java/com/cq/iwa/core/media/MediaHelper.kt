package com.cq.iwa.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cq.iwa.core.permission.PermissionRequester
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun ensureCameraPermission(requester: PermissionRequester): Boolean =
        requester.request(android.Manifest.permission.CAMERA)

    suspend fun compressImage(
        sourceUri: Uri,
        maxSizeKb: Int = 512,
        maxDimension: Int = 1280,
    ): File = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: error("无法读取图片")
        input.use { stream ->
            val original = BitmapFactory.decodeStream(stream)
                ?: error("图片解码失败")
            val scaled = scaleBitmap(original, maxDimension)
            if (scaled != original) original.recycle()

            val outputDir = File(context.cacheDir, "compressed").apply { mkdirs() }
            var quality = 90
            var outputFile: File
            do {
                outputFile = File(outputDir, "img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outputFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
                quality -= 10
            } while (outputFile.length() > maxSizeKb * 1024 && quality > 30)
            scaled.recycle()
            outputFile
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }
}
