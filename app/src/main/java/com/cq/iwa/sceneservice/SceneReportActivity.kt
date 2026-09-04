package com.cq.iwa.sceneservice

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.databinding.ActivitySceneReportBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SceneReportActivity : IwaBaseActivity<ActivitySceneReportBinding>() {

    @Inject
    lateinit var sessionStore: SessionStore

    override fun statusBarColorRes(): Int = R.color.main_background

    override fun inflateBinding(): ActivitySceneReportBinding =
        ActivitySceneReportBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { finish() }
        val id = intent.getStringExtra(SceneServiceNavigator.EXTRA_PRODUCT_ID).orEmpty()
        val token = sessionStore.getRawToken().orEmpty()
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        binding.webView.webViewClient = WebViewClient()
        binding.webView.loadUrl("https://epo.aql.cn/verify?t=$token&p=prs&i=$id")
    }
}
