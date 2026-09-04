package com.cq.iwa.diagnose

import android.os.Bundle
import android.text.InputType
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDiagnoseDalianNbBinding
import com.cq.iwa.feature.diagnose.protocol.DiagnoseCommands
import com.cq.iwa.feature.diagnose.protocol.DiagnoseHex
import com.cq.iwa.feature.diagnose.ui.DalianNbViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DalianNbDiagnoseActivity : IwaBaseActivity<ActivityDiagnoseDalianNbBinding>() {

    private val viewModel: DalianNbViewModel by viewModels()
    private lateinit var spp: DiagnoseSppHelper
    private var currentOrder = 0

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDiagnoseDalianNbBinding =
        ActivityDiagnoseDalianNbBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        bindLabels()
        spp = DiagnoseSppHelper(
            activity = this,
            permissionRequester = permissionRequester,
            scope = lifecycleScope,
            toast = { showToast(it) },
            onState = { binding.btnBluetooth.applyDiagnoseBtState(it)?.let(::showToast) },
            onRead = { onMeterRead(it) },
        )
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBluetooth.setOnClickListener { spp.connectOrToggle() }
        binding.btnQueryWater.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 1
            binding.rowWater.tvValue.text = ""
            spp.write(DiagnoseCommands.getWaterAmount())
        }
        binding.btnFocusSend.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 2
            spp.write(DiagnoseCommands.uploadWaterData())
        }
        binding.btnQueryInfo.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 3
            clearParaValues()
            spp.write(DiagnoseCommands.readDalianNbPara())
        }
        binding.btnReviseReading.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 4
            IwaDialogs.input(
                context = this,
                title = getString(R.string.diagnose_modify_water),
                hint = getString(R.string.diagnose_input_water_hint),
                inputType = InputType.TYPE_CLASS_NUMBER,
            ) { text ->
                when {
                    text.isBlank() -> showToast(getString(R.string.diagnose_input_water))
                    !isWaterReading(text) -> showToast(getString(R.string.diagnose_input_water_hint))
                    else -> spp.write(DiagnoseCommands.modifyWaterData(text.toInt()))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { binding.btnReviseReading.isVisible = it.canWriteWater }
            }
        }
        viewModel.loadPermission()
    }

    private fun onMeterRead(value: ByteArray) {
        if (DiagnoseHex.isWakeup(value)) return
        when (currentOrder) {
            1 -> {
                val result = DiagnoseCommands.parseWaterAmount(value) ?: return
                binding.rowWater.tvValue.text = "${result}L"
            }
            2 -> {
                val result = DiagnoseCommands.parseUploadFlag(value) ?: return
                showToast(if (result.equals("OK", ignoreCase = true)) "强制发送成功" else "强制发送失败")
            }
            3 -> {
                val result = DiagnoseCommands.parseDalianParaRaw(value)
                if (result.isNullOrBlank()) {
                    showToast("获取水表参数失败")
                    return
                }
                val ret = result.split(",")
                if (ret.size != 11) {
                    showToast("返回数据出错")
                    return
                }
                when (ret[1]) {
                    "1" -> {
                        binding.rowResult.tvValue.text = "上报成功"
                        val values = listOf(
                            binding.rowRsrp, binding.rowSnr, binding.rowCover, binding.rowSignal,
                            binding.rowRssi, binding.rowFreq, binding.rowCell, binding.rowCellId, binding.rowTime,
                        )
                        for (i in 2 until 11) {
                            val field = values[i - 2]
                            field.tvValue.text = when (i) {
                                2 -> "${ret[i]} dBm"
                                3 -> (ret[i].toInt() / 10).toString()
                                6 -> "${ret[i]} dBm"
                                else -> ret[i]
                            }
                        }
                    }
                    "2" -> binding.rowResult.tvValue.text = "上报失败"
                    "3" -> binding.rowResult.tvValue.text = "入网失败"
                    "4" -> binding.rowResult.tvValue.text = "暂无结果"
                }
            }
            4 -> {
                val result = DiagnoseCommands.parseWaterAmount(value) ?: return
                binding.rowWater.tvValue.text = "${result}L"
            }
        }
    }

    private fun requireBt(): Boolean {
        if (spp.isConnected()) return true
        showToast("未连接蓝牙设备")
        return false
    }

    private fun bindLabels() {
        binding.rowWater.bind("水表读数：")
        binding.rowResult.bind("上报结果：")
        binding.rowRsrp.bind("信号强度(RSRP)：")
        binding.rowSnr.bind("信噪比：")
        binding.rowCover.bind("覆盖等级：")
        binding.rowSignal.bind("信号等级：")
        binding.rowRssi.bind("RSSI：")
        binding.rowFreq.bind("频点：")
        binding.rowCell.bind("小区ID：")
        binding.rowCellId.bind("CELL_ID：")
        binding.rowTime.bind("获取时间：")
    }

    private fun clearParaValues() {
        listOf(
            binding.rowResult, binding.rowRsrp, binding.rowSnr, binding.rowCover,
            binding.rowSignal, binding.rowRssi, binding.rowFreq, binding.rowCell,
            binding.rowCellId, binding.rowTime,
        ).forEach { it.tvValue.text = "" }
    }

    override fun onDestroy() {
        spp.destroy()
        super.onDestroy()
    }
}
