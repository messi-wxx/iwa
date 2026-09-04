package com.cq.iwa.core.storage

import android.content.Context
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmkvHolder @Inject constructor(
    @ApplicationContext context: Context,
) {
    val default: MMKV = MMKV.defaultMMKV()

    init {
        MmkvInitializer.init(context)
    }
}
