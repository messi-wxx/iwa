package com.cq.iwa.diagnose

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.cq.iwa.R
import com.cq.iwa.databinding.ItemDiagnoseFieldBinding

fun ImageButton.applyDiagnoseBtState(state: DiagnoseSppHelper.SppState): String? {
    val connected = state == DiagnoseSppHelper.SppState.CONNECTED
    ImageViewCompat.setImageTintList(
        this,
        ColorStateList.valueOf(
            ContextCompat.getColor(context, if (connected) R.color.primary else R.color.text_hint),
        ),
    )
    return when (state) {
        DiagnoseSppHelper.SppState.CONNECTING -> "蓝牙连接中..."
        DiagnoseSppHelper.SppState.CONNECTED -> "蓝牙已连接"
        DiagnoseSppHelper.SppState.DISCONNECTED -> "蓝牙已断开"
        DiagnoseSppHelper.SppState.FAILED -> null
    }
}

fun ItemDiagnoseFieldBinding.bind(label: String, value: String = "") {
    tvLabel.text = label
    tvValue.text = value
}

fun isWaterReading(text: String): Boolean = Regex("^[\\d]{1,8}$").matches(text)

fun parseOldReading(raw: String): Int {
    var q = raw.replace("L", "")
    if (q.contains(".")) q = q.substringBefore(".")
    return q.toIntOrNull() ?: 0
}

fun appVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")
}
