package com.cq.iwa

import android.app.Application
import com.cq.iwa.core.logger.TimberPlanter
import com.cq.iwa.core.storage.AppSettings
import com.cq.iwa.core.storage.MmkvInitializer
import com.cq.iwa.sdk.ThirdSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IwaApplication : Application() {

    override fun onCreate() {
        MmkvInitializer.init(this)
        super.onCreate()
        TimberPlanter.plant(this, BuildConfig.DEBUG)
        if (AppSettings().privacyAgreed) {
            ThirdSdk.initAfterPrivacy(this, BuildConfig.DEBUG)
        }
    }
}
