package com.cq.iwa.core.dialog

import android.content.Context

@Deprecated("使用 IwaDialogs")
class DialogBuilder(private val context: Context) {

    private var title: CharSequence? = null
    private var message: CharSequence? = null
    private var positiveText: CharSequence? = null
    private var negativeText: CharSequence? = null
    private var onPositive: (() -> Unit)? = null
    private var onNegative: (() -> Unit)? = null
    private var cancelable: Boolean = true

    fun title(text: CharSequence) = apply { title = text }
    fun message(text: CharSequence) = apply { message = text }
    fun positive(text: CharSequence, action: (() -> Unit)? = null) = apply {
        positiveText = text
        onPositive = action
    }
    fun negative(text: CharSequence, action: (() -> Unit)? = null) = apply {
        negativeText = text
        onNegative = action
    }
    fun cancelable(value: Boolean) = apply { cancelable = value }

    fun show() {
        val msg = message ?: return
        if (negativeText == null) {
            IwaDialogs.message(
                context = context,
                title = title ?: context.getString(com.cq.iwa.core.ui.R.string.core_dialog_hint),
                message = msg,
                confirmText = positiveText ?: context.getString(com.cq.iwa.core.ui.R.string.core_dialog_ok),
                cancelable = cancelable,
                onConfirm = onPositive,
            )
        } else {
            IwaDialogs.confirm(
                context = context,
                title = title ?: context.getString(com.cq.iwa.core.ui.R.string.core_dialog_hint),
                message = msg,
                confirmText = positiveText ?: context.getString(com.cq.iwa.core.ui.R.string.core_dialog_ok),
                cancelText = negativeText ?: context.getString(com.cq.iwa.core.ui.R.string.core_dialog_cancel),
                cancelable = cancelable,
                onConfirm = { onPositive?.invoke() },
                onCancel = onNegative,
            )
        }
    }
}

fun Context.showDialog(block: DialogBuilder.() -> Unit) {
    DialogBuilder(this).apply(block).show()
}
