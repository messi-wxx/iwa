package com.cq.iwa.installation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import com.cq.iwa.R
import com.cq.iwa.feature.installation.ui.InstVFormViewModel
import org.json.JSONObject
import kotlin.math.min

class InstFormDialog(
    context: Context,
    private val title: String,
    private val schema: String,
    private val data: String,
    private val projectId: Int,
    private val formViewModel: InstVFormViewModel,
    private val onToast: (String) -> Unit,
) : Dialog(context) {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var heightAdjusted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_inst_process_form)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setDimAmount(0.45f)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.heightPixels * 0.45).toInt(),
        )
        findViewById<TextView>(R.id.tvDialogTitle).text = title
        findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }
        webView = findViewById(R.id.webView)
        setupWebView()
        webView.loadUrl("file:///android_asset/vform/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
        webView.addJavascriptInterface(Bridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val schemaJs = JSONObject.quote(schema)
                val dataJs = JSONObject.quote(data)
                view?.evaluateJavascript("window.setFormJson($schemaJs, $dataJs, 'readonly')", null)
                handler.postDelayed({ adjustHeight() }, 300)
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    private fun adjustHeight() {
        if (heightAdjusted) return
        webView.evaluateJavascript(
            "Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)",
        ) { value ->
            val contentHeightCss = value.toDoubleOrNull() ?: return@evaluateJavascript
            val scale = if (webView.scale > 0) webView.scale else 1f
            val contentHeightPx = (contentHeightCss * scale).toInt()
            val screenHeight = context.resources.displayMetrics.heightPixels
            val maxHeight = (screenHeight * 0.8).toInt()
            val density = context.resources.displayMetrics.density
            val total = (56 * density).toInt() + contentHeightPx
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                min(total, maxHeight).coerceAtLeast((200 * density).toInt()),
            )
            heightAdjusted = true
        }
    }

    override fun dismiss() {
        if (::webView.isInitialized) {
            webView.apply {
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
        super.dismiss()
    }

    inner class Bridge {
        @JavascriptInterface
        fun onSubmitForm(formDataJson: String) = Unit

        @JavascriptInterface
        fun showToast(message: String) {
            webView.post { onToast(message) }
        }

        @JavascriptInterface
        fun getDictionaryOptionList(paramsJson: String, callbackId: String) {
            val code = runCatching { JSONObject(paramsJson).optString("code") }.getOrDefault("")
            formViewModel.dictionary(code) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun getOptionList(paramsJson: String, callbackId: String) {
            val code = runCatching { JSONObject(paramsJson).optString("code") }.getOrDefault("")
            formViewModel.options(code) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun getMeterInstallInfoList(paramsJson: String, callbackId: String) {
            formViewModel.meterInstallJson(projectId) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun downloadExcel(paramsJson: String, callbackId: String) {
            formViewModel.downloadExcelTemplate()
        }

        @JavascriptInterface
        fun downloadContract(paramsJson: String, callbackId: String) {
            formViewModel.downloadContract(projectId)
        }

        @JavascriptInterface
        fun downloadDocument(paramsJson: String, callbackId: String) {
            val type = runCatching { JSONObject(paramsJson).optString("type").toInt() }.getOrDefault(0)
            formViewModel.downloadDocument(projectId, type)
        }

        @JavascriptInterface
        fun downloadFile(paramsJson: String, callbackId: String) {
            val json = JSONObject(paramsJson)
            formViewModel.downloadFile(json.getString("url"), json.getString("name"))
        }

        private fun callback(id: String, json: String) {
            webView.post { webView.evaluateJavascript("window['$id']($json)", null) }
        }
    }
}
