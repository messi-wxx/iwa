package com.cq.iwa.push

import android.content.Context
import cn.jpush.android.api.CustomMessage
import cn.jpush.android.service.JPushMessageReceiver
import timber.log.Timber

class JpushReceiver : JPushMessageReceiver() {
    override fun onMessage(context: Context?, message: CustomMessage?) {
        super.onMessage(context, message)
        Timber.d("jpush message=%s", message?.message)
    }
}
