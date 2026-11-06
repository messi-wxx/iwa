package com.cq.iwa.dma

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import com.cq.iwa.installation.InstDateRange
import java.util.Calendar

/**
 * 单日选择与报装延期日期同一套 [DatePickerDialog]；日期范围走 [InstDateRange]。
 */
object DmaDates {
    fun pickDay(activity: AppCompatActivity, current: String?, onPicked: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        InstDateRange.parseDay(current)?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            activity,
            { _, year, month, day ->
                onPicked("%04d-%02d-%02d".format(year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }
}
