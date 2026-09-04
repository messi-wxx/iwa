package com.cq.iwa.installation

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import com.cq.iwa.R
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object InstDateRange {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun parseDay(value: String?): Long? {
        val day = value?.take(10).orEmpty()
        if (day.length < 10) return null
        return runCatching { dayFmt.parse(day)?.time }.getOrNull()
    }

    fun formatDay(millis: Long?): String? = millis?.let { dayFmt.format(Date(it)) }

    fun bind(startView: TextView, endView: TextView, startMillis: Long?, endMillis: Long?) {
        bindOne(startView, startMillis, startView.context.getString(R.string.inst_filter_start))
        bindOne(endView, endMillis, endView.context.getString(R.string.inst_filter_end))
    }

    fun pick(
        activity: AppCompatActivity,
        title: CharSequence,
        startMillis: Long?,
        endMillis: Long?,
        onPicked: (Long, Long) -> Unit,
    ) {
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText(title)
        if (startMillis != null && endMillis != null) {
            builder.setSelection(Pair(toUtcDay(startMillis), toUtcDay(endMillis)))
        }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first ?: return@addOnPositiveButtonClickListener
            val end = range.second ?: return@addOnPositiveButtonClickListener
            onPicked(fromUtcDay(start), fromUtcDay(end))
        }
        picker.show(activity.supportFragmentManager, "inst_date_range")
    }

    private fun bindOne(view: TextView, millis: Long?, placeholder: String) {
        if (millis == null) {
            view.text = placeholder
            view.setTextColor(ContextCompat.getColor(view.context, R.color.text_hint))
        } else {
            view.text = dayFmt.format(Date(millis))
            view.setTextColor(ContextCompat.getColor(view.context, R.color.navy))
        }
    }

    private fun toUtcDay(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, local.get(Calendar.YEAR))
            set(Calendar.MONTH, local.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    private fun fromUtcDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utc.get(Calendar.YEAR))
            set(Calendar.MONTH, utc.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
