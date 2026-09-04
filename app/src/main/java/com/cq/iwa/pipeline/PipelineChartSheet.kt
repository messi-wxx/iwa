package com.cq.iwa.pipeline

import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.chart.MpLineCharts
import com.cq.iwa.chart.MpLinePoint
import com.cq.iwa.databinding.DialogPipelineChartBinding
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import com.cq.iwa.feature.pipeline.network.PipelineMonitorDto
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PipelineChartSheet {

    fun show(
        activity: IwaBaseActivity<*>,
        title: String,
        start: String,
        end: String,
        onQuery: (String, String) -> Unit,
        chartFlow: StateFlow<List<PipelineMonitorDto>>,
    ) {
        val binding = DialogPipelineChartBinding.inflate(LayoutInflater.from(activity))
        binding.tvTitle.text = title
        binding.tvStart.text = start
        binding.tvEnd.text = end
        MpLineCharts.setup(binding.chart)
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(binding.root)
        binding.dateRange.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText(title).build()
            picker.addOnPositiveButtonClickListener { range ->
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val nextStart = range.first?.let { fmt.format(Date(it)) } ?: return@addOnPositiveButtonClickListener
                val nextEnd = range.second?.let { fmt.format(Date(it)) } ?: return@addOnPositiveButtonClickListener
                binding.tvStart.text = nextStart
                binding.tvEnd.text = nextEnd
                onQuery(nextStart, nextEnd)
            }
            picker.show(activity.supportFragmentManager, "pipeline_chart_range")
        }
        val owner: LifecycleOwner = activity
        owner.lifecycleScope.launch {
            chartFlow.collect { list ->
                val points = list.reversed().map { item ->
                    MpLinePoint(
                        label = PipelineFormat.fromTimestamp(item.ts, "yyyy-MM-dd HH:mm"),
                        value = item.value?.toFloatOrNull() ?: 0f,
                    )
                }
                MpLineCharts.bind(binding.chart, title, points)
            }
        }
        onQuery(start, end)
        dialog.show()
    }
}
