package com.cq.iwa.installation

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cq.iwa.R
import com.cq.iwa.core.dialog.IwaDialogs
import com.cq.iwa.core.media.CapturePhotoHelper
import com.cq.iwa.databinding.ActivityInstVformBinding
import com.cq.iwa.feature.installation.network.InstActionConfigDto
import com.cq.iwa.feature.installation.network.InstJson
import com.cq.iwa.feature.installation.ui.InstVFormViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

@AndroidEntryPoint
class InstVFormActivity : InstActivity<ActivityInstVformBinding>() {

    private val viewModel: InstVFormViewModel by viewModels()
    private var projectId = 0
    private var taskId = ""
    private var jsonSchema = ""
    private var lastFormData = ""
    private var injected = false
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private lateinit var captureHelper: CapturePhotoHelper
    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadMessage?.onReceiveValue(arrayOf(uri))
        } else {
            uploadMessage?.onReceiveValue(null)
        }
        uploadMessage = null
    }

    override fun inflateBinding() = ActivityInstVformBinding.inflate(layoutInflater)

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        captureHelper = CapturePhotoHelper(this)
        projectId = intent.getIntExtra(InstNavigator.EXTRA_PROJECT_ID, 0)
        taskId = intent.getStringExtra(InstNavigator.EXTRA_TASK_ID).orEmpty()
        jsonSchema = intent.getStringExtra(InstNavigator.EXTRA_SCHEMA).orEmpty()
        lastFormData = intent.getStringExtra(InstNavigator.EXTRA_FORM).orEmpty().takeIf { it.length > 2 }.orEmpty()
        setupInstHeader(
            binding.toolBar,
            intent.getStringExtra(InstNavigator.EXTRA_TITLE).orEmpty().ifBlank { getString(R.string.inst_handle) },
        )
        val actions = InstJson.decode<List<InstActionConfigDto>>(intent.getStringExtra(InstNavigator.EXTRA_ACTIONS)).orEmpty()
        setupButtons(actions)
        setupWebView()
        binding.webView.loadUrl("file:///android_asset/vform/index.html")
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                if (ui.finished) finish()
                for (i in 0 until binding.buttonContainer.childCount) {
                    val button = binding.buttonContainer.getChildAt(i) as? Button ?: continue
                    if (button.text.contains("仪表数量")) button.text = ui.qtyText
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (projectId != 0) viewModel.loadMeterQty(projectId)
    }

    private fun setupButtons(actions: List<InstActionConfigDto>) {
        binding.buttonContainer.removeAllViews()
        actions.forEach { action ->
            val button = Button(this).apply {
                text = if (action.type == "MeterRegistry") {
                    viewModel.loadMeterQty(projectId)
                    "仪表数量"
                } else {
                    action.label
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8
                    marginEnd = 8
                }
                setTextColor(ContextCompat.getColor(this@InstVFormActivity, android.R.color.white))
                backgroundTintList = ContextCompat.getColorStateList(this@InstVFormActivity, R.color.primary)
                setOnClickListener { handleAction(action) }
            }
            binding.buttonContainer.addView(button)
        }
        val complete = Button(this).apply {
            text = getString(R.string.inst_complete)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8
                marginEnd = 8
            }
            setTextColor(ContextCompat.getColor(this@InstVFormActivity, android.R.color.white))
            backgroundTintList = ContextCompat.getColorStateList(this@InstVFormActivity, R.color.primary)
            setOnClickListener {
                binding.webView.evaluateJavascript(
                    "if(typeof window.submitForm === 'function') { window.submitForm(); }",
                    null,
                )
            }
        }
        binding.buttonContainer.addView(complete)
    }

    private fun handleAction(action: InstActionConfigDto) {
        when (action.type.lowercase()) {
            "dismiss" -> {
                if (!requireTaskId()) return
                showReject()
            }
            "delay" -> {
                if (!requireTaskId()) return
                showExtend()
            }
            "meterregistry" -> InstNavigator.openMeters(this, projectId, true)
        }
    }

    private fun requireTaskId(): Boolean {
        if (taskId.isBlank()) {
            showToast("任务ID不能为空")
            return false
        }
        return true
    }

    private fun showReject() {
        viewModel.loadRejectTargets(taskId) { list ->
            if (list.isEmpty()) {
                showToast("没有可用的驳回选项")
                return@loadRejectTargets
            }
            val options = list.map { it.name to it.taskDefKey.orEmpty() }
            val target = InstDialogs.dropdown(this, "请选择驳回节点", options)
            val reason = InstDialogs.field(this, "请输入操作理由")
            InstDialogs.form(this, getString(R.string.inst_reject), buildContent = { box ->
                box.addView(InstDialogs.labeled(this, "驳回节点", target.view))
                box.addView(reason)
            }) {
                if (target.value.isBlank()) {
                    showToast("请选择任务驳回选项")
                    return@form false
                }
                if (reason.text.toString().trim().isEmpty()) {
                    showToast("请输入操作理由")
                    return@form false
                }
                viewModel.reject(taskId, target.value, reason.text.toString().trim())
                true
            }
        }
    }

    private fun showExtend() {
        val reason = InstDialogs.field(this, "请输入操作理由")
        val date = InstDialogs.field(this, "请选择延期日期")
        date.isFocusable = false
        date.isFocusableInTouchMode = false
        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val minCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    date.setText("%04d-%02d-%02d".format(y, m + 1, d))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).apply {
                datePicker.minDate = minCalendar.timeInMillis
            }.show()
        }
        InstDialogs.form(this, getString(R.string.inst_extend), buildContent = { box ->
            box.addView(date)
            box.addView(reason)
        }) {
            if (date.text.toString().trim().isEmpty()) {
                showToast("请选择延期日期")
                return@form false
            }
            if (reason.text.toString().trim().isEmpty()) {
                showToast("请输入操作理由")
                return@form false
            }
            viewModel.extend(taskId, date.text.toString().trim(), reason.text.toString().trim())
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = filePathCallback
                val type = classify(fileChooserParams?.acceptTypes ?: emptyArray())
                lifecycleScope.launch {
                    if (!permissionRequester.request(Manifest.permission.CAMERA)) {
                        uploadMessage?.onReceiveValue(null)
                        uploadMessage = null
                        showToast("需要相机权限才能上传图片")
                        return@launch
                    }
                    showChooser(type)
                }
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (injected) return
                val schema = JSONObject.quote(jsonSchema)
                val data = JSONObject.quote(lastFormData)
                view?.evaluateJavascript("window.setFormJson && window.setFormJson($schema, $data, '')", null)
                injected = true
            }
        }
    }

    private fun classify(acceptTypes: Array<String>): Int {
        val images = setOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico")
        val docs = setOf(".doc", ".docx", ".xls", ".xlsx", ".pdf")
        var image = false
        var doc = false
        acceptTypes.forEach {
            val n = it.trim().lowercase()
            if (n in images) image = true
            if (n in docs) doc = true
        }
        return when {
            image && doc -> 3
            image -> 1
            doc -> 2
            else -> 0
        }
    }

    private fun showChooser(type: Int) {
        val items = when (type) {
            2 -> arrayOf("文件")
            1 -> arrayOf("拍照", "相册")
            else -> arrayOf("拍照", "相册", "文件")
        }
        val dialog = IwaDialogs.list(this, getString(R.string.inst_file), items.map { it }) { which ->
            val label = items[which]
            lifecycleScope.launch {
                when (label) {
                    "拍照" -> {
                        val file = captureHelper.createPictureFile()
                        val uri = captureHelper.capture(file)
                        uploadMessage?.onReceiveValue(uri?.let { arrayOf(it) })
                        uploadMessage = null
                    }
                    "相册" -> {
                        val uri = captureHelper.pickImage()
                        uploadMessage?.onReceiveValue(uri?.let { arrayOf(it) })
                        uploadMessage = null
                    }
                    else -> filePicker.launch("*/*")
                }
            }
        }
        dialog.setOnCancelListener {
            uploadMessage?.onReceiveValue(null)
            uploadMessage = null
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun onSubmitForm(formDataJson: String) {
            runOnUiThread {
                if (!requireTaskId()) return@runOnUiThread
                viewModel.complete(taskId, formDataJson)
            }
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread { this@InstVFormActivity.showToast(message) }
        }

        @JavascriptInterface
        fun getDictionaryOptionList(paramsJson: String, callbackId: String) {
            val code = runCatching { JSONObject(paramsJson).optString("code") }.getOrDefault("")
            viewModel.dictionary(code) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun getOptionList(paramsJson: String, callbackId: String) {
            val code = runCatching { JSONObject(paramsJson).optString("code") }.getOrDefault("")
            viewModel.options(code) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun getMeterInstallInfoList(paramsJson: String, callbackId: String) {
            viewModel.meterInstallJson(projectId) { json -> callback(callbackId, json) }
        }

        @JavascriptInterface
        fun downloadExcel(paramsJson: String, callbackId: String) {
            viewModel.downloadExcelTemplate()
        }

        @JavascriptInterface
        fun downloadContract(paramsJson: String, callbackId: String) {
            viewModel.downloadContract(projectId)
        }

        @JavascriptInterface
        fun downloadDocument(paramsJson: String, callbackId: String) {
            val type = runCatching { JSONObject(paramsJson).optString("type").toInt() }.getOrDefault(0)
            viewModel.downloadDocument(projectId, type)
        }

        @JavascriptInterface
        fun downloadFile(paramsJson: String, callbackId: String) {
            val json = JSONObject(paramsJson)
            viewModel.downloadFile(json.getString("url"), json.getString("name"))
        }
    }

    private fun callback(id: String, json: String) {
        binding.webView.post {
            binding.webView.evaluateJavascript("window['$id']($json)", null)
        }
    }

    override fun onDestroy() {
        binding.webView.apply {
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
