package com.cq.iwa.media

import android.content.Context
import android.content.Intent
import com.cq.iwa.replacemeter.EditEnvImageActivity

object EnvImageNavigator {

    const val EXTRA_PHOTO_PATH = "path"
    const val EXTRA_OUTPUT_PATH = "outputPath"
    const val EXTRA_OVERLAY = "address"

    fun overlayText(code: String, address: String): String =
        if (code.isBlank()) address else "$code---$address"

    fun edit(context: Context, sourcePath: String, outputPath: String, overlayText: String): Intent {
        return Intent(context, EditEnvImageActivity::class.java)
            .putExtra(EXTRA_PHOTO_PATH, sourcePath)
            .putExtra(EXTRA_OUTPUT_PATH, outputPath)
            .putExtra(EXTRA_OVERLAY, overlayText)
    }
}
