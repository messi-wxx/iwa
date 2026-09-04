package com.cq.iwa.legal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import com.cq.iwa.R
import com.cq.iwa.core.ui.base.BaseActivity
import com.cq.iwa.databinding.ActivityWebPageBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebPageActivity : BaseActivity<ActivityWebPageBinding>() {

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun isLightStatusBar(): Boolean = true

    override fun observeNetworkChanges(): Boolean = false

    override fun inflateBinding(): ActivityWebPageBinding =
        ActivityWebPageBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.webView.webViewClient = WebViewClient()
        if (url.isNotBlank()) {
            binding.webView.loadUrl(url)
        }
    }

    override fun onDestroy() {
        binding.webView.stopLoading()
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"

        fun intent(context: Context, title: String, url: String): Intent {
            return Intent(context, WebPageActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_URL, url)
        }
    }
}
