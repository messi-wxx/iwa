package com.cq.iwa.diagnose

import android.os.Bundle
import android.text.InputType
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityDiagnoseWiredBinding
import com.cq.iwa.feature.diagnose.protocol.DiagnoseCommands
import com.cq.iwa.feature.diagnose.protocol.DiagnoseHex
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WiredDiagnoseActivity : IwaBaseActivity<ActivityDiagnoseWiredBinding>() {

    private lateinit var spp: DiagnoseSppHelper
    private var currentOrder = 0

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDiagnoseWiredBinding =
        ActivityDiagnoseWiredBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
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
            spp.write(DiagnoseCommands.getWaterAmount())
        }
        binding.btnSerial.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 2
            spp.write(DiagnoseCommands.uploadWaterData())
        }
        binding.btnVersion.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            currentOrder = 3
            spp.write(DiagnoseCommands.readDalianNbPara())
        }
        binding.btnValveState.setOnClickListener {
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
        binding.btnMore.setOnClickListener {
            if (!requireBt()) return@setOnClickListener
            IwaDialogs.list(
                this,
                getString(R.string.diagnose_more_title),
                listOf("开阀", "关阀", "写申舒斯1X", "写申舒斯10X", "写申舒斯表号", "写申舒斯水量"),
            ) { }
        }
    }

    private fun onMeterRead(value: ByteArray) {
        if (DiagnoseHex.isWakeup(value)) return
        if (currentOrder == 1) {
            val result = DiagnoseCommands.parseWaterAmount(value) ?: return
            binding.rowWater.tvValue.text = "${result}L"
        }
    }

    private fun requireBt(): Boolean {
        if (spp.isConnected()) return true
        showToast("未连接蓝牙设备")
        return false
    }

    private fun bindLabels() {
        binding.rowWater.bind("水表读数：")
        binding.rowSerial.bind("序列号：")
        binding.rowVersion.bind("版本号：")
        binding.rowValve.bind("阀门状态：")
    }

    override fun onDestroy() {
        spp.destroy()
        super.onDestroy()
    }
}
