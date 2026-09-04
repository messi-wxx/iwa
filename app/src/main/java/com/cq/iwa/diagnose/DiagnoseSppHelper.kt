package com.cq.iwa.diagnose

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.core.permission.PermissionRequester
import com.cq.iwa.databinding.DialogBleScanBinding
import com.cq.iwa.databinding.ItemBleDeviceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 经典蓝牙 SPP，对齐老项目 BlueToothUtil + BTManager.createConnection(uuid=null)。
 */
class DiagnoseSppHelper(
    private val activity: FragmentActivity,
    private val permissionRequester: PermissionRequester,
    private val scope: CoroutineScope,
    private val toast: (String) -> Unit,
    private val onState: (SppState) -> Unit,
    private val onRead: (ByteArray) -> Unit,
) {
    enum class SppState { CONNECTING, CONNECTED, DISCONNECTED, FAILED }

    private val main = Handler(Looper.getMainLooper())
    @Suppress("DEPRECATION")
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var dialog: Dialog? = null
    private var listAdapter: SppDeviceAdapter? = null
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var reader: Thread? = null
    @Volatile private var alive = true
    @Volatile private var connected = false

    fun isConnected(): Boolean = connected && socket?.isConnected == true

    fun connectOrToggle() {
        if (!alive) return
        scope.launch { connectOrToggleInternal() }
    }

    fun write(bytes: ByteArray): Boolean {
        if (!isConnected()) {
            toast("未连接蓝牙设备")
            return false
        }
        return runCatching {
            synchronized(this) {
                output?.write(bytes)
                output?.flush()
            }
            true
        }.getOrElse {
            toast("蓝牙发送失败")
            false
        }
    }

    fun destroy() {
        alive = false
        dialog?.dismiss()
        dialog = null
        stopScan()
        disconnect()
    }

    private suspend fun connectOrToggleInternal() {
        if (adapter == null) {
            runOnUi { toast("手机不支持蓝牙") }
            return
        }
        if (isConnected()) {
            disconnect()
            runOnUi { onState(SppState.DISCONNECTED) }
            return
        }
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
            runOnUi { toast("您拒绝了使用此功能必须的定位权限") }
            return
        }
        if (!adapter.isEnabled) {
            runOnUi { toast("请先打开蓝牙") }
            return
        }
        if (!isLocationEnabled()) {
            runOnUi { toast("检查gps是否打开？") }
            return
        }
        runOnUi { startScanUi() }
    }

    @SuppressLint("MissingPermission")
    private fun startScanUi() {
        val binding = DialogBleScanBinding.inflate(activity.layoutInflater)
        val next = Dialog(activity)
        next.requestWindowFeature(Window.FEATURE_NO_TITLE)
        next.setContentView(binding.root)
        next.setOnDismissListener { stopScan() }
        listAdapter = SppDeviceAdapter { device ->
            next.dismiss()
            connect(device)
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(activity)
        binding.rvDevices.adapter = listAdapter
        dialog = next
        next.show()
        next.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.45f)
            val width = (activity.resources.displayMetrics.widthPixels * 0.86f).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
        registerReceiver()
        val started = runCatching { adapter?.startDiscovery() }.getOrDefault(false)
        if (started != true) {
            toast("检查gps是否打开？")
        }
    }

    private val foundReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    val name = runCatching { device.name }.getOrNull()
                    if (!name.isNullOrBlank()) {
                        runOnUi { listAdapter?.add(device) }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> Unit
            }
        }
    }

    private var receiverRegistered = false

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(foundReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            activity.registerReceiver(foundReceiver, filter)
        }
        receiverRegistered = true
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        runCatching { adapter?.cancelDiscovery() }
        if (receiverRegistered) {
            runCatching { activity.unregisterReceiver(foundReceiver) }
            receiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        stopScan()
        runOnUi { onState(SppState.CONNECTING) }
        Thread {
            try {
                val sock = openSocket(device)
                sock.connect()
                socket = sock
                input = sock.inputStream
                output = sock.outputStream
                connected = true
                runOnUi { onState(SppState.CONNECTED) }
                startReader()
            } catch (_: Exception) {
                disconnect()
                runOnUi {
                    onState(SppState.FAILED)
                    toast("蓝牙连接失败")
                }
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(device: BluetoothDevice): BluetoothSocket {
        runCatching { adapter?.cancelDiscovery() }
        return runCatching {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        }.getOrElse {
            @Suppress("UNCHECKED_CAST")
            device.javaClass
                .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                .invoke(device, 1) as BluetoothSocket
        }
    }

    private fun startReader() {
        reader = Thread {
            val buffer = ByteArray(1024)
            while (alive && connected) {
                val count = runCatching { input?.read(buffer) }.getOrNull() ?: break
                if (count <= 0) break
                val data = buffer.copyOf(count)
                runOnUi { if (alive) onRead(data) }
            }
            if (alive && connected) {
                disconnect()
                runOnUi { onState(SppState.DISCONNECTED) }
            }
        }.also { it.start() }
    }

    private fun disconnect() {
        connected = false
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        input = null
        output = null
        socket = null
        reader = null
    }

    private fun runOnUi(block: () -> Unit) {
        if (!alive || activity.isFinishing || activity.isDestroyed) return
        main.post {
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

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

private class SppDeviceAdapter(
    private val onClick: (BluetoothDevice) -> Unit,
) : RecyclerView.Adapter<SppDeviceAdapter.Holder>() {
    private val items = mutableListOf<BluetoothDevice>()

    @SuppressLint("MissingPermission")
    fun add(device: BluetoothDevice) {
        if (items.any { it.address == device.address }) return
        items.add(device)
        notifyItemInserted(items.lastIndex)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBleDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name ?: "未知设备"
        holder.binding.tvMac.text = item.address
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class Holder(val binding: ItemBleDeviceBinding) : RecyclerView.ViewHolder(binding.root)
}
