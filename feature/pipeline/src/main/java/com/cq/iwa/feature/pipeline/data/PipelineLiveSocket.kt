package com.cq.iwa.feature.pipeline.data

import com.cq.iwa.feature.pipeline.network.PipelineFollowDeviceDto
import com.cq.iwa.feature.pipeline.network.PipelineJson
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.network.PipelineSubscribeParam
import com.cq.iwa.feature.pipeline.network.PipelineTsSubCmd
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineLiveSocket @Inject constructor() {

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var url: String = ""
    private var reconnectCount = 0
    private val maxReconnect = 15
    @Volatile private var opening = false
    @Volatile private var lastSubscribeJson: String? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages

    fun connect(url: String) {
        if (url.isBlank()) return
        this.url = url
        if (_connected.value || opening) return
        opening = true
        if (client == null) {
            client = OkHttpClient.Builder()
                .pingInterval(15, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
        webSocket = client?.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    opening = false
                    this@PipelineLiveSocket.webSocket = webSocket
                    _connected.value = true
                    reconnectCount = 0
                    flushSubscribe()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    _messages.tryEmit(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    opening = false
                    _connected.value = false
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    opening = false
                    _connected.value = false
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    opening = false
                    _connected.value = false
                    scheduleReconnect()
                }
            },
        )
    }

    fun disconnect() {
        reconnectCount = maxReconnect
        lastSubscribeJson = null
        opening = false
        webSocket?.close(1000, "Normal closure")
        _connected.value = false
    }

    fun send(text: String): Boolean {
        if (!_connected.value) return false
        return webSocket?.send(text) == true
    }

    @Synchronized
    private fun flushSubscribe() {
        val payload = lastSubscribeJson ?: return
        if (_connected.value) {
            webSocket?.send(payload)
        }
    }

    fun subscribeMetrics(metrics: List<PipelineMetricDto>) {
        if (metrics.isEmpty()) return
        val cmds = metrics.map { metric ->
            PipelineTsSubCmd(
                cmdId = metric.sourceSiteId,
                entityId = metric.sourceIotId,
                keys = metric.metricId,
                entityType = if (metric.sourceSiteType == 1) "ASSET" else "DEVICE",
            )
        }
        val merged = cmds.groupBy { it.entityId }.map { (entityId, group) ->
            val first = group.first()
            PipelineTsSubCmd(
                cmdId = first.cmdId,
                entityType = first.entityType,
                entityId = entityId,
                keys = group.flatMap { it.keys.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(","),
            )
        }
        lastSubscribeJson = PipelineJson.encode(PipelineSubscribeParam(merged))
        flushSubscribe()
    }

    fun applyTelemetry(text: String, list: List<PipelineFollowDeviceDto>): List<PipelineFollowDeviceDto> {
        val root = runCatching { PipelineJson.json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return list
        val subscriptionId = (root["subscriptionId"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return list
        val data = root["data"] as? JsonObject ?: return list
        return list.map { site ->
            val metrics = site.siteMetrics.map { metric ->
                if (metric.sourceSiteId == subscriptionId && data.containsKey(metric.metricId)) {
                    val pair = readValuePair(data[metric.metricId])
                    metric.copy(value = pair.second, timestamp = pair.first)
                } else {
                    metric
                }
            }
            site.copy(siteMetrics = metrics)
        }
    }

    fun applyTelemetryToMetrics(text: String, metrics: List<PipelineMetricDto>): List<PipelineMetricDto> {
        val root = runCatching { PipelineJson.json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return metrics
        val subscriptionId = (root["subscriptionId"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return metrics
        val data = root["data"] as? JsonObject ?: return metrics
        return metrics.map { metric ->
            if (metric.sourceSiteId == subscriptionId && data.containsKey(metric.metricId)) {
                val pair = readValuePair(data[metric.metricId])
                metric.copy(value = pair.second, timestamp = pair.first)
            } else {
                metric
            }
        }
    }

    private fun readValuePair(element: kotlinx.serialization.json.JsonElement?): Pair<Long, String> {
        val firstRow = (element as? JsonArray)?.firstOrNull() as? JsonArray ?: return 0L to ""
        val ts = firstRow.getOrNull(0)?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        val value = firstRow.getOrNull(1)?.jsonPrimitive?.contentOrNull.orEmpty()
        return ts to value
    }

    private fun scheduleReconnect() {
        if (reconnectCount >= maxReconnect || url.isBlank()) return
        reconnectCount++
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!_connected.value) connect(url)
        }, 2000L)
    }
}
