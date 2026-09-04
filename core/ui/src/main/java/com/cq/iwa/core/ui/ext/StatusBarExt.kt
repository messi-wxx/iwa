package com.cq.iwa.core.ui.ext

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 让状态栏颜色与页面背景衔接，避免色块突兀。
 * @param lightBackground true 时用深色状态栏图标（浅色页面）
 */
fun Activity.applyStatusBar(
    @ColorInt color: Int,
    lightBackground: Boolean = true,
) {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    window.statusBarColor = color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.navigationBarColor = color
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightBackground
        isAppearanceLightNavigationBars = lightBackground
    }
}

fun Activity.applyTransparentStatusBar(lightBackground: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = lightBackground
}
