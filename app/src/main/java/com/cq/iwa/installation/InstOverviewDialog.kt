package com.cq.iwa.installation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import com.cq.iwa.R
import com.cq.iwa.core.ui.toast.ToastUtils
import org.json.JSONObject

class InstOverviewDialog(
    context: Context,
    private val xml: String,
    private val highlightedNodeIds: List<String>,
    private val activeNodeIds: List<String>,
) : Dialog(context) {

    private lateinit var webView: WebView
    private lateinit var loadingView: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_inst_overview)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setDimAmount(0.45f)
        window?.let {
            val layoutParams = it.attributes
            layoutParams.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            layoutParams.height = (context.resources.displayMetrics.heightPixels * 0.6).toInt()
            it.attributes = layoutParams
        }
        webView = findViewById(R.id.webView)
        loadingView = findViewById(R.id.loadingView)
        findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }
        setupWebView()
        loadProcessDiagram()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.setOnTouchListener { _, _ -> false }
        webView.addJavascriptInterface(BpmnBridge(), "Android")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                renderBpmnDiagram()
            }
        }
    }

    private fun loadProcessDiagram() {
        val htmlContent = context.assets.open("process.html").bufferedReader().use { it.readText() }
        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            htmlContent,
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun renderBpmnDiagram() {
        if (xml.isBlank()) {
            loadingView.visibility = View.GONE
            ToastUtils.show(context, "流程图数据为空")
            return
        }
        startTimeout()
        val escapedXml = JSONObject.quote(xml)
        val highlightArrayStr = highlightedNodeIds.joinToString(",") { JSONObject.quote(it) }
        val activeArrayStr = activeNodeIds.joinToString(",") { JSONObject.quote(it) }
        webView.evaluateJavascript(
            "renderBpmn($escapedXml, [$highlightArrayStr], [$activeArrayStr])",
            null,
        )
    }

    private fun startTimeout() {
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (loadingView.visibility == View.VISIBLE) {
                loadingView.visibility = View.GONE
                ToastUtils.show(context, "流程图加载超时，请重试")
            }
        }
        handler.postDelayed(timeoutRunnable!!, 8000L)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    inner class BpmnBridge {
        @JavascriptInterface
        fun onBpmnRenderComplete() {
            handler.post {
                cancelTimeout()
                loadingView.visibility = View.GONE
            }
        }

        @JavascriptInterface
        fun onBpmnRenderError(error: String) {
            handler.post {
                cancelTimeout()
                loadingView.visibility = View.GONE
                ToastUtils.show(context, "流程图加载失败: $error")
            }
        }
    }

    override fun dismiss() {
        cancelTimeout()
        if (::webView.isInitialized) {
            webView.apply {
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
        super.dismiss()
    }
}
