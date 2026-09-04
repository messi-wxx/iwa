package com.cq.iwa.replacemeter

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.media.PhotoProcessor
import com.cq.iwa.databinding.ActivityEditEnvImageBinding
import com.cq.iwa.media.EnvImageNavigator
import com.cq.iwa.replacemeter.doodle.DoodleView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class EditEnvImageActivity : IwaBaseActivity<ActivityEditEnvImageBinding>() {

    private var canRevert = false
    private var sourcePath: String = ""

    override fun statusBarColorRes(): Int = R.color.navy

    override fun isLightStatusBar(): Boolean = false

    override fun inflateBinding(): ActivityEditEnvImageBinding =
        ActivityEditEnvImageBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        sourcePath = intent.getStringExtra(EnvImageNavigator.EXTRA_PHOTO_PATH).orEmpty()
        val overlay = intent.getStringExtra(EnvImageNavigator.EXTRA_OVERLAY).orEmpty()
        if (sourcePath.isBlank() || !File(sourcePath).exists()) {
            showToast(getString(R.string.replacemeter_edit_env_fail))
            finish()
            return
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnRevert.setOnClickListener {
            if (canRevert) {
                binding.doodleView.revert()
            } else {
                showToast(getString(R.string.replacemeter_doodle_select_first))
            }
        }
        binding.graphPicker.setOnCheckedChangeListener { _, checkedId ->
            binding.doodleView.setGraphType(
                if (checkedId == R.id.graphArrow) DoodleView.GraphType.ARROW else DoodleView.GraphType.OVAL,
            )
        }
        binding.doodleView.onRevertChanged = { canRevert = it }
        binding.doodleView.setEditable(true)
        binding.doodleView.setOverlayText(overlay)
        binding.doodleView.post {
            val bitmap = runCatching { PhotoProcessor.decodeOriented(sourcePath) }.getOrNull()
            if (bitmap == null) {
                showToast(getString(R.string.replacemeter_edit_env_fail))
                finish()
            } else {
                fitBitmap(bitmap)
            }
        }
    }

    private fun fitBitmap(bitmap: Bitmap) {
        val containerW = binding.doodleContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val containerH = binding.doodleContainer.height.takeIf { it > 0 } ?: bitmap.height
        val ratio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
        val targetW = containerW
        val targetH = (targetW / ratio).toInt().coerceAtMost(containerH)
        val params = binding.doodleView.layoutParams
        params.width = targetW
        params.height = targetH.coerceAtLeast(1)
        binding.doodleView.layoutParams = params
        binding.doodleView.post { binding.doodleView.setOriginBitmap(bitmap) }
    }

    private fun save() {
        showLoading(getString(R.string.replacemeter_saving_image))
        lifecycleScope.launch {
            val output = intent.getStringExtra(EnvImageNavigator.EXTRA_OUTPUT_PATH)
                ?: File(cacheDir, "env_${System.currentTimeMillis()}.jpg").absolutePath
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = binding.doodleView.exportBitmap()
                    val file = File(output)
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }
                    if (!bitmap.isRecycled) bitmap.recycle()
                    PhotoProcessor.compressIfNeeded(file)
                    val source = File(sourcePath)
                    if (source.exists() && source.absolutePath != file.absolutePath) source.delete()
                }.isSuccess
            }
            hideLoading()
            if (ok) {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(EnvImageNavigator.EXTRA_PHOTO_PATH, output),
                )
                finish()
            } else {
                showToast(getString(R.string.replacemeter_edit_env_fail))
            }
        }
    }
}
