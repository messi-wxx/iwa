package com.cq.iwa.core.storage

import android.content.Context
import com.tencent.mmkv.MMKV

object MmkvInitializer {

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                MMKV.initialize(context.applicationContext)
                initialized = true
            }
        }
    }
}
