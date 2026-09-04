plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cq.iwa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cq.iwa"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "v1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["JPUSH_PKGNAME"] = "com.cq.iwa"
        manifestPlaceholders["JPUSH_APPKEY"] = "359b5e18ce73e66d5ff95181"
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
        manifestPlaceholders["HONOR_APPID"] = "104573467"
        manifestPlaceholders["HUAWEI_APPID"] = "118207091"
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    applicationVariants.all {
        val variant = this
        outputs.map { output ->
            val outputImpl = output as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (outputImpl != null) {
                if (variant.buildType.name == "release") {
                    outputImpl.outputFileName = "iwa_${variant.versionName}.apk"
                } else {
                    outputImpl.outputFileName = "iwa_${variant.versionName}_debug.apk"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:logger"))
    implementation(project(":core:storage"))
    implementation(project(":core:image"))
    implementation(project(":core:permission"))
    implementation(project(":core:media"))
    implementation(project(":core:dialog"))
    implementation(project(":core:monitor"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:readmeter"))
    implementation(project(":feature:replacemeter"))
    implementation(project(":feature:calibration"))
    implementation(project(":feature:urgepayment"))
    implementation(project(":feature:diagnose"))
    implementation(project(":feature:sceneservice"))
    implementation(project(":feature:pipeline"))
    implementation(project(":feature:installation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.amap.map)
//    implementation(libs.amap.location)
    implementation(libs.fastble)
    implementation(libs.zxing.android.embedded)
    implementation(libs.refresh.kernel)
    implementation(libs.refresh.header)
    implementation(libs.refresh.footer)
    implementation(libs.mp.chart)
    implementation(libs.bugly)
    implementation(libs.jpush)
    implementation(libs.jpush.honor)
    implementation(libs.jpush.huawei)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

apply(plugin = "com.huawei.agconnect")
