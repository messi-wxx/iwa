package com.cq.iwa.installation

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.dialog.R as DialogR
import com.cq.iwa.core.ui.R as CoreUiR
import com.cq.iwa.feature.installation.network.InstTableDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object InstDialogs {

    fun applyCardWindow(dialog: Dialog, widthRatio: Float = 0.86f, heightRatio: Float? = null) {
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.45f)
            val metrics = dialog.context.resources.displayMetrics
            val width = (metrics.widthPixels * widthRatio).toInt()
            val height = heightRatio?.let { (metrics.heightPixels * it).toInt() }
                ?: ViewGroup.LayoutParams.WRAP_CONTENT
            window.setLayout(width, height)
            window.setGravity(Gravity.CENTER)
        }
    }

    fun form(
        context: Context,
        title: CharSequence,
        confirmText: CharSequence = context.getString(R.string.inst_confirm),
        cancelText: CharSequence = context.getString(R.string.inst_cancel),
        buildContent: (LinearLayout) -> Unit,
        onConfirm: () -> Boolean,
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_inst_form, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val container = view.findViewById<LinearLayout>(R.id.layoutDialogContent)
        buildContent(container)
        view.findViewById<TextView>(R.id.btnDialogNegative).apply {
            text = cancelText
            setOnClickListener { dialog.dismiss() }
        }
        view.findViewById<TextView>(R.id.btnDialogPositive).apply {
            text = confirmText
            setOnClickListener {
                if (onConfirm()) dialog.dismiss()
            }
        }
        dialog.show()
        applyCardWindow(dialog)
        return dialog
    }

    fun table(context: Context, table: InstTableDto): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_inst_table, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)
        view.findViewById<TextView>(R.id.tvDialogTitle).text = table.tableName.orEmpty()
        val tableLayout = view.findViewById<TableLayout>(R.id.tableLayout)
        val fields = table.queryFields
        if (fields.isNotEmpty()) {
            tableLayout.addView(tableRow(context, fields.map { it.displayName }, header = true))
            table.datas.forEach { row ->
                tableLayout.addView(
                    tableRow(context, fields.map { jsonCell(row, it.showField) }, header = false),
                )
            }
        }
        view.findViewById<TextView>(R.id.btnDialogPositive).setOnClickListener { dialog.dismiss() }
        dialog.show()
        applyCardWindow(dialog, widthRatio = 0.92f)
        return dialog
    }

    fun field(context: Context, hint: String, value: String = "", inputType: Int? = null): EditText {
        val pad = (12 * context.resources.displayMetrics.density).toInt()
        return EditText(context).apply {
            this.hint = hint
            setText(value)
            background = ContextCompat.getDrawable(context, DialogR.drawable.bg_dialog_input)
            setPadding(pad, pad, pad, pad)
            setTextColor(ContextCompat.getColor(context, CoreUiR.color.core_dialog_title))
            setHintTextColor(ContextCompat.getColor(context, CoreUiR.color.core_dialog_hint))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (8 * context.resources.displayMetrics.density).toInt()
            }
            if (inputType != null) this.inputType = inputType
        }
    }

    class Dropdown(
        val view: TextView,
        private val options: List<Pair<String, String>>,
        selected: String,
    ) {
        var value: String = selected
            private set

        init {
            val match = options.firstOrNull { it.second == selected || it.first == selected }
            val initial = match ?: if (selected.isBlank()) options.firstOrNull() else null
            if (initial != null) {
                value = initial.second
                view.text = initial.first
            }
        }

        fun select(index: Int) {
            val option = options.getOrNull(index) ?: return
            value = option.second
            view.text = option.first
        }
    }

    fun dropdown(
        context: Context,
        hint: String,
        options: List<Pair<String, String>>,
        selected: String = "",
    ): Dropdown {
        val pad = (12 * context.resources.displayMetrics.density).toInt()
        val view = TextView(context).apply {
            this.hint = hint
            background = ContextCompat.getDrawable(context, DialogR.drawable.bg_dialog_input)
            setPadding(pad, pad, pad, pad)
            setTextColor(ContextCompat.getColor(context, CoreUiR.color.core_dialog_title))
            setHintTextColor(ContextCompat.getColor(context, CoreUiR.color.core_dialog_hint))
            textSize = 15f
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0)
            compoundDrawablePadding = pad / 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (8 * context.resources.displayMetrics.density).toInt()
            }
        }
        val dropdown = Dropdown(view, options, selected)
        view.setOnClickListener {
            if (options.isEmpty()) return@setOnClickListener
            IwaDialogs.list(context, hint, options.map { it.first }) { index ->
                dropdown.select(index)
            }
        }
        return dropdown
    }

    fun labeled(context: Context, label: String, child: View): LinearLayout {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = (8 * context.resources.displayMetrics.density).toInt()
            }
        }
        box.addView(TextView(context).apply {
            text = label
            setTextColor(ContextCompat.getColor(context, CoreUiR.color.core_dialog_body))
            textSize = 13f
        })
        box.addView(child)
        return box
    }

    private fun tableRow(context: Context, cells: List<String>, header: Boolean): TableRow {
        val row = TableRow(context)
        val pad = (8 * context.resources.displayMetrics.density).toInt()
        cells.forEach { text ->
            row.addView(TextView(context).apply {
                this.text = text
                setPadding(pad, pad, pad, pad)
                setTextColor(ContextCompat.getColor(context, if (header) R.color.navy else CoreUiR.color.core_dialog_body))
                textSize = if (header) 13f else 12f
                minWidth = (72 * context.resources.displayMetrics.density).toInt()
                gravity = Gravity.CENTER
            })
        }
        return row
    }

    fun jsonCell(obj: JsonObject, key: String): String {
        val element = obj[key] ?: return ""
        return (element as? JsonPrimitive)?.contentOrNull?.orEmpty()
            ?: element.toString().trim('"')
    }
}
