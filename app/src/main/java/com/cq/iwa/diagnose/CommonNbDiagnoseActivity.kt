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
import com.cq.iwa.databinding.ActivityDiagnoseCommonNbBinding
import com.cq.iwa.feature.diagnose.protocol.DiagnoseCommands
import com.cq.iwa.feature.diagnose.protocol.DiagnoseHex
import com.cq.iwa.feature.diagnose.protocol.NbParameter
import com.cq.iwa.feature.diagnose.ui.CommonNbViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommonNbDiagnoseActivity : IwaBaseActivity<ActivityDiagnoseCommonNbBinding>() {

    private val viewModel: CommonNbViewModel by viewModels()
    private lateinit var spp: DiagnoseSppHelper
    private var currentOrder = 0
    private var parameter = NbParameter()
    private var updateReading = ""
    private var oldReading = 0

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityDiagnoseCommonNbBinding =
        ActivityDiagnoseCommonNbBinding.inflate(layoutInflater)

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
        viewModel.onWriteCommand = { bytes -> spp.write(bytes) }
        viewModel.onWriteVerified = { value -> binding.rowWater.tvValue.text = "${value}L" }
        viewModel.onWriteRollback = { value -> binding.rowWater.tvValue.text = "${value}L" }
        viewModel.onBusyChanged = { busy ->
            if (busy) showLoading(getString(R.string.diagnose_busy)) else hideLoading()
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBluetooth.setOnClickListener { spp.connectOrToggle() }
        binding.btnQueryInfo.setOnClickListener {
            if (!spp.isConnected()) {
                showToast("未连接蓝牙设备")
                return@setOnClickListener
            }
            currentOrder = 1
            parameter = NbParameter()
            clearValues()
            spp.write(DiagnoseCommands.readCommonNbPara())
        }
        binding.btnFocusSend.setOnClickListener {
            if (!spp.isConnected()) {
                showToast("未连接蓝牙设备")
                return@setOnClickListener
            }
            currentOrder = 2
            spp.write(DiagnoseCommands.uploadWaterData())
        }
        binding.btnOpenValve.setOnClickListener {
            if (!spp.isConnected()) {
                showToast("未连接蓝牙设备")
                return@setOnClickListener
            }
            currentOrder = 3
            spp.write(DiagnoseCommands.openValve())
        }
        binding.btnCloseValve.setOnClickListener {
            if (!spp.isConnected()) {
                showToast("未连接蓝牙设备")
                return@setOnClickListener
            }
            currentOrder = 4
            spp.write(DiagnoseCommands.closeValve())
        }
        binding.btnReviseReading.setOnClickListener { reviseReading() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { binding.btnReviseReading.isVisible = it.canWriteWater }
            }
        }
        viewModel.loadPermission()
    }

    private fun reviseReading() {
        val meterCode = binding.rowMeterCode.tvValue.text?.toString().orEmpty()
        if (meterCode.isBlank()) {
            showToast("水表编号为空，请先读取水表参数")
            return
        }
        if (!spp.isConnected()) {
            showToast("未连接蓝牙设备")
            return
        }
        oldReading = parseOldReading(binding.rowWater.tvValue.text?.toString().orEmpty())
        currentOrder = 5
        IwaDialogs.input(
            context = this,
            title = getString(R.string.diagnose_modify_water),
            hint = getString(R.string.diagnose_input_water_hint),
            inputType = InputType.TYPE_CLASS_NUMBER,
        ) { text ->
            when {
                text.isBlank() -> showToast(getString(R.string.diagnose_input_water))
                !isWaterReading(text) -> showToast(getString(R.string.diagnose_input_water_hint))
                else -> {
                    updateReading = text
                    viewModel.prepareModifyWater(meterCode, text)
                }
            }
        }
    }

    private fun onMeterRead(value: ByteArray) {
        if (DiagnoseHex.isWakeup(value)) return
        when (currentOrder) {
            1 -> {
                parameter = DiagnoseCommands.parseCommonNbPara(value, parameter) ?: return
                binding.rowWater.tvValue.text = parameter.waterQty
                binding.rowValve.tvValue.text = parameter.valveSate
                binding.rowMeterCode.tvValue.text = parameter.meterCode
                binding.rowPeriod.tvValue.text = parameter.reportingPeriod
                binding.rowIp.tvValue.text = parameter.ip
                binding.rowPort.tvValue.text = parameter.port
                binding.rowProtocol.tvValue.text = parameter.agreementNo
                binding.rowWork.tvValue.text = parameter.workingParameters
                binding.rowSensor.tvValue.text = parameter.sensorNo
                binding.rowDate.tvValue.text = parameter.date
                binding.rowNb.tvValue.text = parameter.nbNo
                binding.rowImei.tvValue.text = parameter.imei
                binding.rowImsi.tvValue.text = parameter.imsi
            }
            2 -> {
                val result = DiagnoseCommands.parseUploadFlag(value) ?: return
                showToast(if (result.equals("OK", ignoreCase = true)) "强制发送成功" else "强制发送失败")
            }
            5 -> {
                if (!spp.isConnected()) {
                    hideLoading()
                    showToast("修改失败,蓝牙已断开")
                    return
                }
                viewModel.onModifyEcho(
                    meterCode = binding.rowMeterCode.tvValue.text?.toString().orEmpty(),
                    echoed = DiagnoseCommands.parseWaterAmount(value),
                    expected = updateReading,
                    oldReading = oldReading,
                    appVersion = appVersionName(this),
                )
            }
        }
    }

    private fun bindLabels() {
        binding.rowWater.bind("水表读数：")
        binding.rowValve.bind("阀门状态：")
        binding.rowMeterCode.bind("水表编号：")
        binding.rowPeriod.bind("上报周期：")
        binding.rowIp.bind("IP：")
        binding.rowPort.bind("端口号：")
        binding.rowProtocol.bind("协议号：")
        binding.rowWork.bind("工作参数：")
        binding.rowSensor.bind("传感器版本号：")
        binding.rowDate.bind("日期：")
        binding.rowNb.bind("NB模块版本号：")
        binding.rowImei.bind("IMEI：")
        binding.rowImsi.bind("IMSI：")
    }

    private fun clearValues() {
        listOf(
            binding.rowWater, binding.rowValve, binding.rowMeterCode, binding.rowPeriod,
            binding.rowIp, binding.rowPort, binding.rowProtocol, binding.rowWork,
            binding.rowSensor, binding.rowDate, binding.rowNb, binding.rowImei, binding.rowImsi,
        ).forEach { it.tvValue.text = "" }
    }

    override fun onDestroy() {
        spp.destroy()
        super.onDestroy()
    }
}
