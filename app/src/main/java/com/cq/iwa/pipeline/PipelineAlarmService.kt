package com.cq.iwa.pipeline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cq.iwa.R
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import com.cq.iwa.feature.pipeline.data.PipelineRepository
import com.cq.iwa.feature.pipeline.data.PipelineSessionStore
import com.cq.iwa.feature.pipeline.network.PipelineAlarmMessageDto
import com.cq.iwa.feature.pipeline.network.PipelineJson
import com.cq.iwa.feature.pipeline.network.PipelineTreeItemDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PipelineAlarmService : Service() {

    @Inject lateinit var repository: PipelineRepository
    @Inject lateinit var session: PipelineSessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var connectAt = 0L
    private val shown = mutableSetOf<String>()
    private var reconnectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startAsForeground()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iwa:pipeline-alarm").apply {
            acquire(10 * 60 * 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connect()
        return START_STICKY
    }

    override fun onDestroy() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "stop")
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification = serviceNotification(getString(R.string.pipeline_alarm_service), false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFY_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFY_SERVICE, notification)
        }
    }

    private fun connect() {
        val url = session.webSocketUrl
        if (url.isBlank()) {
            scheduleReconnect()
            return
        }
        updateService(getString(R.string.splash_ready), false)
        webSocket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connectAt = System.currentTimeMillis()
                    updateService(getString(R.string.pipeline_alarm_connected), true)
                    subscribeAlarms()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    updateService("连接已断开", false)
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    updateService("连接失败", false)
                    scheduleReconnect()
                }
            },
        )
    }

    private fun subscribeAlarms() {
        scope.launch {
            val result = repository.getSiteTree()
            val tree = result.getOrNull().orEmpty()
            val ids = collectDeviceIotIds(tree)
            if (ids.isEmpty()) return@launch
            val body = """
                {
                    "alarmDataCmds": [{
                        "query": {
                            "entityFilter": {
                                "type": "entityList",
                                "entityType": "DEVICE",
                                "entityList": ${PipelineJson.encode(ids)}
                            },
                            "pageLink": {
                                "page": 0,
                                "pageSize": 10000,
                                "timeWindow": 259200000000,
                                "statusList": ["ACTIVE"],
                                "sortOrder": {
                                    "key": {"key": "createdTime", "type": "ALARM_FIELD"},
                                    "direction": "DESC"
                                }
                            },
                            "alarmFields": [],
                            "latestValues": []
                        },
                        "cmdId": 9999999
                    }]
                }
            """.trimIndent()
            webSocket?.send(body)
        }
    }

    private fun handleMessage(text: String) {
        val message = PipelineJson.decode<PipelineAlarmMessageDto>(text) ?: return
        if (message.cmdId != 9999999) return
        message.data?.data.orEmpty().forEach { alarm ->
            if (alarm.createdTime <= connectAt) return@forEach
            val id = alarm.id.id
            if (!shown.add(id)) return@forEach
            val time = PipelineFormat.fromTimestamp(alarm.createdTime, "yyyy-MM-dd HH:mm:ss")
            val content = "${alarm.originatorName}  $time".let {
                if (it.length > 50) it.take(50) + "..." else it
            }
            showAlarm(alarm.type, content)
        }
    }

    private fun showAlarm(title: String, content: String) {
        val intent = Intent(this, PipelineAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGE)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000)
            connect()
        }
    }

    private fun collectDeviceIotIds(tree: List<PipelineTreeItemDto>): List<String> {
        val result = mutableListOf<String>()
        fun walk(node: PipelineTreeItemDto) {
            if (node.type == 3) result.add(node.iotId)
            node.children.forEach(::walk)
        }
        tree.forEach(::walk)
        return result
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, getString(R.string.pipeline_alarm_service), NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MESSAGE, getString(R.string.pipeline_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            },
        )
    }

    private fun updateService(status: String, connected: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFY_SERVICE, serviceNotification(status, connected))
    }

    private fun serviceNotification(status: String, connected: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.pipeline_alarm_service))
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(if (connected) Color.GREEN else Color.RED)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val NOTIFY_SERVICE = 31
        private const val CHANNEL_SERVICE = "pipeline_ws_service"
        private const val CHANNEL_MESSAGE = "pipeline_ws_message"

        fun start(context: Context) {
            val intent = Intent(context, PipelineAlarmService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
