package com.cq.iwa.replacemeter

import android.app.Dialog
import android.bluetooth.BluetoothGatt
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.cq.iwa.BuildConfig
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.permission.PermissionRequester
import com.cq.iwa.databinding.DialogBleScanBinding
import com.cq.iwa.databinding.ItemBleDeviceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ReplaceBleHelper(
    private val activity: FragmentActivity,
    private val permissionRequester: PermissionRequester,
    private val scope: CoroutineScope,
    private val toast: (String) -> Unit,
    private val onCode: (String) -> Unit,
) {
    private var dialog: Dialog? = null
    private var adapter: BleDeviceAdapter? = null
    @Volatile private var alive = true

    init {
        initManager()
        connectedDevice()?.let { bindNotify(it, quiet = true) }
    }

    fun connectOrScan() {
        if (!alive) return
        initManager()
        when {
            !BleManager.getInstance().isSupportBle -> toast(activity.getString(R.string.replacemeter_ble_unsupported))
            !BleManager.getInstance().isBlueEnable -> toast(activity.getString(R.string.replacemeter_ble_off))
            else -> {
                val connected = connectedDevice()
                if (connected != null) {
                    bindNotify(connected)
                    IwaDialogs.confirm(
                        context = activity,
                        title = activity.getString(R.string.replacemeter_ble_connected_title),
                        message = activity.getString(R.string.replacemeter_ble_connected_message),
                        confirmText = activity.getString(R.string.replacemeter_ble_keep),
                        cancelText = activity.getString(R.string.replacemeter_ble_disconnect_scan),
                        onConfirm = {},
                        onCancel = {
                            disconnectAll()
                            scope.launch { scan() }
                        },
                    )
                } else {
                    scope.launch { scan() }
                }
            }
        }
    }

    fun destroy() {
        alive = false
        dialog?.dismiss()
        dialog = null
        runCatching { BleManager.getInstance().cancelScan() }
    }

    private fun initManager() {
        BleManager.getInstance().init(activity.application)
        BleManager.getInstance()
            .enableLog(BuildConfig.DEBUG)
            .setReConnectCount(1, 5000)
            .setOperateTimeout(5000)
    }

    private fun connectedDevice(): BleDevice? {
        return BleManager.getInstance().allConnectedDevice?.firstOrNull()
    }

    private fun disconnectAll() {
        runCatching { BleManager.getInstance().disconnectAllDevice() }
        BleConfig.isConnection = false
    }

    private suspend fun scan() {
        val permissions = buildList {
            add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_SCAN)
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        val denied = permissionRequester.request(*permissions.toTypedArray())
        if (denied.isNotEmpty()) {
            toast("您拒绝了使用此功能必须的权限")
            return
        }
        if (!isLocationEnabled()) {
            toast(activity.getString(R.string.replacemeter_ble_need_location))
            return
        }
        BleManager.getInstance().initScanRule(BleScanRuleConfig.Builder().setScanTimeOut(10_000).build())
        showDialog()
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                runOnUi { adapter?.clear() }
            }

            override fun onScanning(bleDevice: BleDevice) {
                runOnUi { adapter?.add(bleDevice) }
            }

            override fun onScanFinished(scanResultList: MutableList<BleDevice>?) {
                if (scanResultList.isNullOrEmpty()) {
                    runOnUi { toast(activity.getString(R.string.replacemeter_ble_empty)) }
                }
            }
        })
    }

    private fun showDialog() {
        val binding = DialogBleScanBinding.inflate(activity.layoutInflater)
        val next = Dialog(activity)
        next.requestWindowFeature(Window.FEATURE_NO_TITLE)
        next.setContentView(binding.root)
        next.setOnDismissListener { BleManager.getInstance().cancelScan() }
        adapter = BleDeviceAdapter { device ->
            next.dismiss()
            connect(device)
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(activity)
        binding.rvDevices.adapter = adapter
        dialog = next
        next.show()
        next.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.45f)
            val width = (activity.resources.displayMetrics.widthPixels * 0.86f).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }

    private fun connect(device: BleDevice) {
        BleManager.getInstance().connect(device, object : BleGattCallback() {
            override fun onStartConnect() {
                BleConfig.isConnection = false
            }

            override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                BleConfig.isConnection = false
                runOnUi { toast(activity.getString(R.string.replacemeter_ble_connect_fail)) }
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                BleConfig.isConnection = true
                bindNotify(bleDevice, gatt)
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                device: BleDevice?,
                gatt: BluetoothGatt?,
                status: Int,
            ) {
                BleConfig.isConnection = false
            }
        })
    }

    private fun bindNotify(
        device: BleDevice,
        gatt: BluetoothGatt? = BleManager.getInstance().getBluetoothGatt(device),
        quiet: Boolean = false,
    ) {
        val services = gatt?.services
        if (services == null || services.size <= 2) {
            if (!quiet) runOnUi { toast(activity.getString(R.string.replacemeter_ble_notify_fail)) }
            return
        }
        val service = services[2]
        val chars = service.characteristics
        if (chars == null || chars.size <= 1) {
            if (!quiet) runOnUi { toast(activity.getString(R.string.replacemeter_ble_notify_fail)) }
            return
        }
        val serviceUuid = service.uuid.toString()
        val charUuid = chars[1].uuid.toString()
        runCatching { BleManager.getInstance().stopNotify(device, serviceUuid, charUuid) }
        notify(device, serviceUuid, charUuid)
    }

    private fun notify(device: BleDevice, serviceUuid: String, charUuid: String) {
        BleManager.getInstance().notify(device, serviceUuid, charUuid, object : BleNotifyCallback() {
            override fun onNotifySuccess() = Unit
            override fun onNotifyFailure(exception: BleException?) {
                runOnUi { toast(activity.getString(R.string.replacemeter_ble_notify_fail)) }
            }

            override fun onCharacteristicChanged(data: ByteArray?) {
                val code = data?.toString(Charsets.UTF_8)?.trim().orEmpty()
                if (code.isNotBlank()) runOnUi { onCode(code) }
            }
        })
    }

    private fun runOnUi(block: () -> Unit) {
        if (!alive || activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            if (alive && !activity.isFinishing && !activity.isDestroyed) block()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val manager = activity.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                activity.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF,
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}

object BleConfig {
    var isConnection: Boolean = false
}

private class BleDeviceAdapter(
    private val onClick: (BleDevice) -> Unit,
) : RecyclerView.Adapter<BleDeviceAdapter.Holder>() {
    private val items = mutableListOf<BleDevice>()

    fun add(device: BleDevice) {
        if (items.any { it.mac == device.mac }) return
        items.add(device)
        notifyItemInserted(items.lastIndex)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBleDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name ?: "未知设备"
        holder.binding.tvMac.text = item.mac
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemBleDeviceBinding) : RecyclerView.ViewHolder(binding.root)
}
