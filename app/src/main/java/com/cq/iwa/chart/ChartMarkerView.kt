package com.cq.iwa.chart

import android.content.Context
import android.widget.TextView
import com.cq.iwa.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class ChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val points: List<MpLinePoint>,
) : MarkerView(context, layoutResource) {

    private val tvTime: TextView by lazy { findViewById(R.id.tvTime) }
    private val tvValue: TextView by lazy { findViewById(R.id.tvValue) }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()
            if (index in points.indices) {
                val data = points[index]
                tvTime.text = data.label
                tvValue.text = "数值: ${data.value}"
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
