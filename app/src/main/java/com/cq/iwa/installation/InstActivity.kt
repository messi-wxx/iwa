package com.cq.iwa.installation

import androidx.viewbinding.ViewBinding
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R

abstract class InstActivity<VB : ViewBinding> : IwaBaseActivity<VB>() {
    override fun statusBarColorRes(): Int = R.color.main_background
    override fun isLightStatusBar(): Boolean = true
}
