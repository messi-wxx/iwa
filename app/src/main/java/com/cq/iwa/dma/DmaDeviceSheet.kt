package com.cq.iwa.dma

import android.view.LayoutInflater
import com.cq.iwa.chart.MpLineCharts
import com.cq.iwa.chart.MpLinePoint
import com.cq.iwa.databinding.DialogDmaDeviceBinding
import com.cq.iwa.feature.dma.ui.DmaDeviceUi
import com.google.android.material.bottomsheet.BottomSheetDialog

object DmaDeviceSheet {

    fun show(activity: DmaDetailActivity, device: DmaDeviceUi, date: String) {
        val binding = DialogDmaDeviceBinding.inflate(LayoutInflater.from(activity))
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(binding.root)
        binding.tvTitle.text = device.name.ifBlank { activity.getString(com.cq.iwa.R.string.dma_device_title) }
        binding.tvMeta.text = listOf(device.direction, device.code, device.caliber, device.typeName, device.online, date)
            .filter { it.isNotBlank() }
            .joinToString("  ")
        binding.tvStats.text = listOf(
            "日累计 ${device.cumulative.ifBlank { "—" }}",
            "平均 ${device.average.ifBlank { "—" }}",
            "最大 ${device.maxText.ifBlank { "—" }}",
            "最小 ${device.minText.ifBlank { "—" }}",
        ).joinToString("\n")
        MpLineCharts.setup(binding.chart)
        val points = device.hours.mapIndexed { index, value ->
            MpLinePoint("%02d:00".format(index), value)
        }
        MpLineCharts.bind(binding.chart, device.name, points)
        binding.chart.visibility = if (points.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
