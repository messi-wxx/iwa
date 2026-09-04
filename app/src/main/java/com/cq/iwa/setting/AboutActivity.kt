package com.cq.iwa.setting

import android.graphics.Paint
import android.os.Bundle
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityAboutBinding
import com.cq.iwa.legal.Legal
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : IwaBaseActivity<ActivityAboutBinding>() {

    override fun statusBarColorRes(): Int? = R.color.main_background

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivityAboutBinding =
        ActivityAboutBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvVersion.text = getString(
            R.string.settings_version,
            runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName
            }.getOrNull().orEmpty(),
        )
        binding.rowPrivacyPolicy.setOnClickListener {
            Legal.openPrivacyPolicy(this)
        }
        binding.tvIcpNo.paintFlags = binding.tvIcpNo.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvIcpNo.setOnClickListener { Legal.openBeian(this) }
        binding.tvIcpPortal.paintFlags = binding.tvIcpPortal.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvIcpPortal.setOnClickListener { Legal.openBeian(this) }
    }
}
