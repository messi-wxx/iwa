package com.cq.iwa.feature.readmeter.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterLocalStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun accountDir(customerCode: String, userCode: String): File {
        val root = context.getExternalFilesDir("pictures") ?: File(context.filesDir, "pictures")
        return File(root, sanitize("${customerCode}_$userCode")).apply { mkdirs() }
    }

    fun createPictureFile(customerCode: String, userCode: String): File {
        return File(accountDir(customerCode, userCode), "meter_${UUID.randomUUID()}.jpg")
    }

    fun resolveLocalFile(path: String, customerCode: String, userCode: String): File? {
        val direct = File(path)
        if (direct.exists() && direct.isFile) return direct
        val name = File(path).name
        val inAccount = File(accountDir(customerCode, userCode), name)
        if (inAccount.exists() && inAccount.isFile) return inAccount
        val root = context.getExternalFilesDir("pictures")
        val legacy = root?.let { File(it, name) }
        if (legacy?.exists() == true && legacy.isFile) return legacy
        return null
    }

    fun clearAccountPictures(customerCode: String, userCode: String) {
        val dir = accountDir(customerCode, userCode)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun accountPictureSize(customerCode: String, userCode: String): Long {
        return folderSize(accountDir(customerCode, userCode))
    }

    private fun folderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { folderSize(it) } ?: 0L
    }

    private fun sanitize(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
