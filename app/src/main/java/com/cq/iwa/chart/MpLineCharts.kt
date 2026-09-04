package com.cq.iwa.chart

import android.graphics.Color
import com.cq.iwa.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

data class MpLinePoint(
    val label: String,
    val value: Float,
)

object MpLineCharts {

    fun setup(chart: LineChart) {
        chart.apply {
            setDrawBorders(true)
            setBorderWidth(1f)
            setBorderColor(Color.LTGRAY)
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            animateX(1500)
            animateY(1500)
        }
        chart.legend.apply {
            form = Legend.LegendForm.LINE
            textColor = Color.BLACK
            textSize = 12f
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
        }
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.DKGRAY
            textSize = 10f
            setDrawGridLines(true)
            gridColor = Color.parseColor("#EEEEEE")
            granularity = 1f
            labelCount = 5
            setAvoidFirstLastClipping(true)
        }
        chart.axisLeft.apply {
            textColor = Color.DKGRAY
            textSize = 10f
            setDrawGridLines(true)
            gridColor = Color.parseColor("#EEEEEE")
            axisMinimum = 0f
            spaceTop = 15f
        }
        chart.axisRight.isEnabled = false
    }

    fun bind(chart: LineChart, title: String, points: List<MpLinePoint>) {
        if (points.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }
        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.value)
        }
        val dataSet = LineDataSet(entries, title).apply {
            color = Color.parseColor("#2196F3")
            lineWidth = 2f
            circleRadius = 2f
            setDrawCircleHole(false)
            valueTextSize = 10f
            valueTextColor = Color.DKGRAY
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
            setDrawVerticalHighlightIndicator(true)
            highLightColor = Color.parseColor("#FF4081")
            highlightLineWidth = 1f
        }
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index == 0 || index == points.lastIndex) {
                    return points.getOrNull(index)?.label.orEmpty()
                }
                return ""
            }
        }
        chart.data = LineData(dataSet)
        val markerView = ChartMarkerView(chart.context, R.layout.chart_marker, points)
        markerView.chartView = chart
        chart.marker = markerView
        chart.invalidate()
    }
}
