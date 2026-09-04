package com.cq.iwa.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoProcessor {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    suspend fun addTimeWatermark(source: File, dest: File = source): File = withContext(Dispatchers.IO) {
        val bitmap = decodeOriented(source.absolutePath)
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (mutable != bitmap) bitmap.recycle()
        val canvas = Canvas(mutable)
        PhotoOverlay.drawTimestamp(
            canvas = canvas,
            imageWidth = mutable.width,
            imageHeight = mutable.height,
            text = timeFormat.format(Date()),
        )
        writeJpeg(mutable, dest, 100)
        if (!mutable.isRecycled) mutable.recycle()
        dest
    }

    suspend fun compressLocalFile(path: String): Boolean {
        if (path.isBlank() || path == "button") return true
        val file = File(path)
        if (!file.isFile) return true
        return runCatching { compressIfNeeded(file) }.isSuccess
    }

    suspend fun compressIfNeeded(
        source: File,
        dest: File = source,
        ignoreBelowKb: Int = 200,
        maxDimension: Int = 1920,
    ): File = withContext(Dispatchers.IO) {
        if (source.exists() && source.length() <= ignoreBelowKb * 1024L && dest == source) {
            return@withContext source
        }
        val original = decodeOriented(source.absolutePath)
        val scaled = scale(original, maxDimension)
        if (scaled != original) original.recycle()
        var quality = 90
        do {
            writeJpeg(scaled, dest, quality)
            quality -= 10
        } while (dest.length() > ignoreBelowKb * 1024L && quality > 30)
        if (!scaled.isRecycled) scaled.recycle()
        dest
    }

    fun decodeOriented(path: String): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 4096)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(path, options)
            ?: error("图片解码失败")
        val exif = runCatching { ExifInterface(path) }.getOrNull() ?: return bitmap
        val degrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun scale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        while (width / sample > maxSize || height / sample > maxSize) {
            sample *= 2
        }
        return sample
    }

    private fun writeJpeg(bitmap: Bitmap, dest: File, quality: Int) {
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
        }
    }
}
