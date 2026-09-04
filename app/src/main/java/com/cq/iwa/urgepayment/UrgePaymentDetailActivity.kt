package com.cq.iwa.urgepayment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.databinding.ActivityUrgePaymentDetailBinding
import com.cq.iwa.databinding.ItemUrgeDeviceBinding
import com.cq.iwa.databinding.ItemUrgeFeeBinding
import com.cq.iwa.feature.urgepayment.ui.UrgeDeviceItemUi
import com.cq.iwa.feature.urgepayment.ui.UrgeDetailUi
import com.cq.iwa.feature.urgepayment.ui.UrgeFeeItemUi
import com.cq.iwa.feature.urgepayment.ui.UrgePaymentDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UrgePaymentDetailActivity : IwaBaseActivity<ActivityUrgePaymentDetailBinding>() {

    private val viewModel: UrgePaymentDetailViewModel by viewModels()
    private val feeAdapter = UrgeFeeAdapter()
    private val deviceAdapter = UrgeDeviceAdapter(
        onHistory = { viewModel.loadReadHistory(it) },
        onValve = { confirmValve(it) },
        onRemark = { device -> showRemarkInput(device) },
    )

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivityUrgePaymentDetailBinding =
        ActivityUrgePaymentDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val clientCode = intent.getStringExtra(UrgePaymentNavigator.EXTRA_CLIENT_CODE).orEmpty()
        viewModel.onReadHistory = { items -> showUrgeHistoryDialog(this, items) }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.btnPrev.setOnClickListener { viewModel.previous() }
        binding.btnNext.setOnClickListener { viewModel.next() }
        binding.tvCall.setOnClickListener {
            val phone = binding.tvPhone.text?.toString().orEmpty()
            if (phone.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
        binding.tvEditPhone.setOnClickListener {
            IwaDialogs.input(
                context = this,
                title = getString(R.string.urge_edit_phone),
                hint = getString(R.string.urge_phone_hint),
                value = binding.tvPhone.text?.toString().orEmpty(),
                inputType = InputType.TYPE_CLASS_PHONE,
                onConfirm = { viewModel.changePhone(it) },
            )
        }
        binding.rvFees.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDevices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFees.adapter = feeAdapter
        binding.rvDevices.adapter = deviceAdapter
        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            showLoadingOverlay = false,
            onSuccess = { ui ->
                binding.bottomBar.isVisible = true
                bind(ui)
            },
            onRetry = { viewModel.load(clientCode) },
        )
        if (clientCode.isBlank()) {
            showToast(getString(R.string.urge_missing_param))
            finish()
            return
        }
        viewModel.load(clientCode)
    }

    private fun bind(ui: UrgeDetailUi) {
        binding.tvUserName.text = ui.name
        binding.tvUserCode.text = ui.code
        binding.tvPhone.text = ui.phone
        binding.tvCall.isVisible = ui.phone.isNotBlank()
        binding.tvAddress.text = ui.address
        binding.tvUserState.text = ui.state
        binding.tvOpenDate.text = ui.openDate
        binding.tvUserRemark.text = ui.remark
        binding.tvBalance.text = ui.balance
        binding.tvOweFee.text = ui.oweFee
        binding.rvFees.isVisible = ui.hasFees
        binding.tvNoFee.isVisible = !ui.hasFees
        binding.rvDevices.isVisible = ui.hasDevices
        binding.tvNoMeter.isVisible = !ui.hasDevices
        feeAdapter.submit(ui.fees)
        deviceAdapter.submit(ui.devices)
    }

    private fun confirmValve(deviceId: Int) {
        IwaDialogs.confirm(
            context = this,
            title = getString(R.string.urge_valve_title),
            message = getString(R.string.urge_valve_message),
            confirmText = getString(R.string.urge_open_valve),
            cancelText = getString(R.string.urge_close_valve),
            onConfirm = { viewModel.openValve(deviceId) },
            onCancel = { viewModel.closeValve(deviceId) },
        )
    }

    private fun showRemarkInput(device: UrgeDeviceItemUi) {
        IwaDialogs.input(
            context = this,
            title = getString(R.string.urge_edit_remark),
            hint = getString(R.string.urge_remark_hint),
            value = device.remark,
            onConfirm = { viewModel.changeRemark(device.deviceId, it) },
        )
    }
}

private class UrgeFeeAdapter : RecyclerView.Adapter<UrgeFeeAdapter.Holder>() {
    private var items: List<UrgeFeeItemUi> = emptyList()

    fun submit(list: List<UrgeFeeItemUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvMonth.text = item.month
        holder.binding.tvMeter.text = item.meter
        holder.binding.tvStartQty.text = item.startQty
        holder.binding.tvEndQty.text = item.endQty
        holder.binding.tvUseQty.text = item.useQty
        holder.binding.tvReceivable.text = item.receivableFee
        holder.binding.tvLateFee.text = item.lateFee
    }

    class Holder(val binding: ItemUrgeFeeBinding) : RecyclerView.ViewHolder(binding.root)
}

private class UrgeDeviceAdapter(
    private val onHistory: (Int) -> Unit,
    private val onValve: (Int) -> Unit,
    private val onRemark: (UrgeDeviceItemUi) -> Unit,
) : RecyclerView.Adapter<UrgeDeviceAdapter.Holder>() {

    private var items: List<UrgeDeviceItemUi> = emptyList()

    fun submit(list: List<UrgeDeviceItemUi>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUrgeDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvMeterCode.text = item.meterCode
        holder.binding.tvCaliber.text = item.caliber
        holder.binding.tvFeeKind.text = item.feeKind
        holder.binding.tvFeeState.text = item.feeState
        holder.binding.tvChargeWay.text = item.chargeWay
        holder.binding.tvReading.text = item.reading
        holder.binding.tvReadDate.text = item.readDate
        holder.binding.tvValveState.text = item.valveState
        holder.binding.tvUseState.text = item.useState
        holder.binding.tvBook.text = item.bookName
        holder.binding.tvRemark.text = item.remark
        holder.binding.tvReadHistory.setOnClickListener { onHistory(item.deviceId) }
        holder.binding.tvSwitchValve.setOnClickListener { onValve(item.deviceId) }
        holder.binding.tvEditRemark.setOnClickListener { onRemark(item) }
        holder.binding.tvRemark.setOnClickListener { onRemark(item) }
    }

    class Holder(val binding: ItemUrgeDeviceBinding) : RecyclerView.ViewHolder(binding.root)
}
