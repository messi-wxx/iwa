package com.cq.iwa.core.network.interceptor

import android.util.Log
import com.cq.iwa.core.network.BuildConfig
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug 网络日志：
 * - 文件上传 / 二进制 / multipart 只打文件名、类型、大小，不打二进制流
 * - 文本/JSON 按 Logcat 单条上限分段打印，避免超长被截断
 */
@Singleton
class HttpDebugInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) return chain.proceed(chain.request())

        val request = chain.request()
        logRequest(request)

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            log("<-- HTTP FAILED: ${e.message}")
            throw e
        }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        logResponse(response, tookMs)
        return response
    }

    private fun logRequest(request: Request) {
        val body = request.body
        log("--> ${request.method} ${request.url}")
        logHeaders(request.headers)
        when {
            body == null -> log("--> END ${request.method}")
            body.isDuplex() || body.isOneShot() ->
                log("--> END ${request.method} (one-shot/duplex body omitted)")
            body is MultipartBody -> logMultipart(body, request.method)
            shouldOmitBody(body.contentType()) -> {
                log(
                    "(binary body omitted: type=${body.contentType()}, " +
                        "size=${formatSize(body.contentLength())})",
                )
                log("--> END ${request.method}")
            }
            else -> {
                val buffer = Buffer()
                body.writeTo(buffer)
                val size = buffer.size
                if (!buffer.isProbablyUtf8()) {
                    log("(binary body omitted: type=${body.contentType()}, size=${formatSize(size)})")
                } else {
                    logBody(buffer.readString(charsetOf(body.contentType())))
                }
                log("--> END ${request.method} (${formatSize(body.contentLength().takeIf { it >= 0 } ?: size)})")
            }
        }
    }

    private fun logResponse(response: Response, tookMs: Long) {
        val body = response.body
        log("<-- ${response.code} ${response.message} ${response.request.url} (${tookMs}ms)")
        logHeaders(response.headers)
        when {
            body == null || !response.hasDecodableBody() -> log("<-- END HTTP")
            shouldOmitBody(body.contentType()) -> {
                log(
                    "(binary body omitted: type=${body.contentType()}, " +
                        "size=${formatSize(body.contentLength())})",
                )
                log("<-- END HTTP")
            }
            bodyEncoded(response.headers) -> log("<-- END HTTP (encoded body omitted)")
            else -> {
                val source = body.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer.clone()
                val size = buffer.size
                if (!buffer.isProbablyUtf8()) {
                    log("(binary ${formatSize(size)} body omitted)")
                } else {
                    logBody(buffer.readString(charsetOf(body.contentType())))
                }
                log("<-- END HTTP (${formatSize(body.contentLength().takeIf { it >= 0 } ?: size)})")
            }
        }
    }

    private fun logMultipart(body: MultipartBody, method: String) {
        log("(multipart body omitted: ${body.parts.size} parts, ${formatSize(body.contentLength())})")
        body.parts.forEachIndexed { index, part ->
            val disposition = part.headers?.get("Content-Disposition").orEmpty()
            val partBody = part.body
            log(
                "  part[$index] $disposition type=${partBody.contentType()} " +
                    "size=${formatSize(partBody.contentLength())}",
            )
        }
        log("--> END $method")
    }

    private fun logHeaders(headers: Headers) {
        for (i in 0 until headers.size) {
            log("${headers.name(i)}: ${headers.value(i)}")
        }
    }

    private fun logBody(text: String) {
        if (text.isEmpty()) return
        val chunks = splitUtf8(text, CHUNK_BYTES)
        if (chunks.size == 1) {
            log(text)
            return
        }
        chunks.forEachIndexed { index, chunk ->
            log("(${index + 1}/${chunks.size}) $chunk")
        }
        log("(body ${formatSize(text.toByteArray(StandardCharsets.UTF_8).size.toLong())}, split into ${chunks.size} parts)")
    }

    private fun log(message: String) {
        val chunks = splitUtf8(message, CHUNK_BYTES)
        if (chunks.size == 1) {
            Log.d(TAG, message)
            return
        }
        chunks.forEachIndexed { index, chunk ->
            Log.d(TAG, "(${index + 1}/${chunks.size}) $chunk")
        }
    }

    companion object {
        private const val TAG = "OkHttp"
        /** Logcat 单条约 4KB，预留分段前缀后按 3500 字节切分 */
        private const val CHUNK_BYTES = 3500
    }
}

private fun shouldOmitBody(contentType: MediaType?): Boolean {
    val type = contentType?.type.orEmpty()
    val subtype = contentType?.subtype.orEmpty()
    return type == "multipart" ||
        type == "image" ||
        type == "video" ||
        type == "audio" ||
        subtype.contains("octet-stream", ignoreCase = true) ||
        subtype.contains("zip", ignoreCase = true) ||
        subtype.contains("gzip", ignoreCase = true) ||
        subtype.contains("pdf", ignoreCase = true) ||
        subtype.contains("protobuf", ignoreCase = true)
}

private fun bodyEncoded(headers: Headers): Boolean {
    val encoding = headers["Content-Encoding"] ?: return false
    return !encoding.equals("identity", ignoreCase = true) &&
        !encoding.equals("gzip", ignoreCase = true)
}

private fun charsetOf(contentType: MediaType?): Charset =
    contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

private fun formatSize(bytes: Long): String = if (bytes < 0) "unknown" else "$bytes bytes"

private fun Response.hasDecodableBody(): Boolean {
    if (request.method.equals("HEAD", ignoreCase = true)) return false
    val code = this.code
    return code !in 100..199 && code != 204 && code != 304
}

private fun Buffer.isProbablyUtf8(): Boolean = try {
    val prefix = Buffer()
    copyTo(prefix, 0, minOf(size, 64L))
    repeat(16) {
        if (prefix.exhausted()) return true
        val codePoint = prefix.readUtf8CodePoint()
        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
    }
    true
} catch (_: Exception) {
    false
}

private fun splitUtf8(text: String, maxBytes: Int): List<String> {
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    if (bytes.size <= maxBytes) return listOf(text)
    val parts = ArrayList<String>()
    var offset = 0
    while (offset < bytes.size) {
        var end = minOf(offset + maxBytes, bytes.size)
        if (end < bytes.size) {
            while (end > offset && (bytes[end].toInt() and 0xC0) == 0x80) {
                end--
            }
        }
        if (end == offset) end = minOf(offset + 1, bytes.size)
        parts += String(bytes, offset, end - offset, StandardCharsets.UTF_8)
        offset = end
    }
    return parts
}
