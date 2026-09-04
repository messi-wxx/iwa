package com.cq.iwa.core.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.text.method.ScrollingMovementMethod
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.core.ui.R as CoreUiR

internal fun Dialog.applyCardWindow(widthRatio: Float = 0.86f) {
    window?.let { window ->
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0.45f)
        val width = (context.resources.displayMetrics.widthPixels * widthRatio).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }
}

object IwaDialogs {

    fun confirm(
        context: Context,
        title: CharSequence = context.getString(CoreUiR.string.core_dialog_hint),
        message: CharSequence,
        confirmText: CharSequence = context.getString(CoreUiR.string.core_dialog_ok),
        cancelText: CharSequence = context.getString(CoreUiR.string.core_dialog_cancel),
        cancelable: Boolean = true,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
        dialog.setContentView(view)
        dialog.setCancelable(cancelable)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        bindScrollableMessage(view.findViewById(R.id.tvDialogMessage), message)
        val negative = view.findViewById<TextView>(R.id.btnDialogNegative)
        val positive = view.findViewById<TextView>(R.id.btnDialogPositive)
        negative.text = cancelText
        positive.text = confirmText
        negative.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }
        positive.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
        dialog.applyCardWindow()
        return dialog
    }

    fun confirmWithCheck(
        context: Context,
        title: CharSequence = context.getString(CoreUiR.string.core_dialog_hint),
        message: CharSequence,
        checkText: CharSequence,
        confirmText: CharSequence = context.getString(CoreUiR.string.core_dialog_ok),
        cancelText: CharSequence = context.getString(CoreUiR.string.core_dialog_cancel),
        cancelable: Boolean = true,
        onConfirm: () -> Unit,
        onCancel: (checked: Boolean) -> Unit,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
        dialog.setContentView(view)
        dialog.setCancelable(cancelable)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        view.findViewById<TextView>(R.id.tvDialogMessage).text = message
        val checkBox = view.findViewById<CheckBox>(R.id.cbDialogOption)
        checkBox.isVisible = true
        checkBox.text = checkText
        val negative = view.findViewById<TextView>(R.id.btnDialogNegative)
        val positive = view.findViewById<TextView>(R.id.btnDialogPositive)
        negative.text = cancelText
        positive.text = confirmText
        negative.setOnClickListener {
            val checked = checkBox.isChecked
            dialog.dismiss()
            onCancel(checked)
        }
        positive.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
        dialog.applyCardWindow()
        return dialog
    }

    fun message(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        confirmText: CharSequence = context.getString(CoreUiR.string.core_dialog_ok),
        cancelable: Boolean = true,
        onConfirm: (() -> Unit)? = null,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_message, null)
        dialog.setContentView(view)
        dialog.setCancelable(cancelable)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        bindScrollableMessage(view.findViewById(R.id.tvDialogMessage), message)
        val positive = view.findViewById<TextView>(R.id.btnDialogPositive)
        positive.text = confirmText
        positive.setOnClickListener {
            dialog.dismiss()
            onConfirm?.invoke()
        }
        dialog.show()
        dialog.applyCardWindow()
        return dialog
    }

    fun input(
        context: Context,
        title: CharSequence,
        hint: CharSequence? = null,
        value: CharSequence = "",
        message: CharSequence? = null,
        confirmText: CharSequence = context.getString(CoreUiR.string.core_dialog_ok),
        cancelText: CharSequence = context.getString(CoreUiR.string.core_dialog_cancel),
        inputType: Int? = null,
        onConfirm: (String) -> Unit,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_input, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val messageView = view.findViewById<TextView>(R.id.tvDialogMessage)
        messageView.isVisible = !message.isNullOrBlank()
        messageView.text = message
        val input = view.findViewById<EditText>(R.id.etDialogInput)
        input.hint = hint
        input.setText(value)
        input.setSelection(input.text?.length ?: 0)
        if (inputType != null) input.inputType = inputType
        view.findViewById<TextView>(R.id.btnDialogNegative).apply {
            text = cancelText
            setOnClickListener { dialog.dismiss() }
        }
        view.findViewById<TextView>(R.id.btnDialogPositive).apply {
            text = confirmText
            setOnClickListener {
                dialog.dismiss()
                onConfirm(input.text?.toString().orEmpty().trim())
            }
        }
        dialog.show()
        dialog.applyCardWindow()
        input.requestFocus()
        return dialog
    }

    fun list(
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        onSelect: (Int) -> Unit,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val recycler = view.findViewById<RecyclerView>(R.id.rvDialogList)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = DialogListAdapter(items) { index ->
            dialog.dismiss()
            onSelect(index)
        }
        dialog.show()
        dialog.applyCardWindow()
        return dialog
    }

    private fun bindScrollableMessage(view: TextView, message: CharSequence) {
        view.text = message
        val hasLink = message is android.text.Spanned &&
            message.getSpans(0, message.length, android.text.style.ClickableSpan::class.java).isNotEmpty()
        view.movementMethod = if (hasLink) {
            android.text.method.LinkMovementMethod.getInstance()
        } else {
            ScrollingMovementMethod.getInstance()
        }
    }
}

private class DialogListAdapter(
    private val items: List<CharSequence>,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<DialogListAdapter.Holder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_list, parent, false) as TextView
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.view.text = items[position]
        holder.view.setOnClickListener { onClick(position) }
    }

    class Holder(val view: TextView) : RecyclerView.ViewHolder(view)
}
