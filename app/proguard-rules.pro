# Retrofit（泛型签名必须保留，对应旧 IWA 的 Signature / Gson TypeToken）
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Kotlinx Serialization（替代旧 IWA 的 Gson 实体 Keep）
-dontnote kotlinx.serialization.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class **$$serializer { *; }
-keep class com.cq.iwa.**.FlexibleStringSerializer { *; }
-keep class com.cq.iwa.**.FlexibleLongSerializer { *; }
-keep class com.cq.iwa.core.network.model.** { *; }
-keep class com.cq.iwa.feature.**.network.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# MMKV
-keep class com.tencent.mmkv.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Coil
-keep class coil.** { *; }

# AMap（对齐已验证的 WMService 规则；com.autonavi 是地图 native 必要包）
-keep class com.amap.api.maps.** { *; }
-keep class com.amap.api.trace.** { *; }
-keep class com.amap.api.location.** { *; }
-keep class com.amap.api.fence.** { *; }
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.autonavi.aps.amapapi.model.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# FastBle
-keep class com.clj.fastble.** { *; }
-dontwarn com.clj.fastble.**

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Crash stack lines for Bugly
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.** { *; }

# JPush
-dontwarn cn.jpush.**
-keep class cn.jpush.** { *; }
-keep class * extends cn.jpush.android.service.JPushMessageReceiver { *; }
-dontwarn cn.jiguang.**
-keep class cn.jiguang.** { *; }

# Honor push
-dontwarn com.hihonor.push.**
-keep class com.hihonor.push.** { *; }

# Huawei HMS / AGC
-dontwarn com.huawei.hms.**
-dontwarn com.huawei.agconnect.**
-keep class com.huawei.hms.** { *; }
-keep class com.huawei.hmf.** { *; }
-keep class com.huawei.agconnect.** { *; }
-keep class com.hianalytics.android.** { *; }
-keep class com.huawei.updatesdk.** { *; }
-keep class com.huawei.push.** { *; }

# WebView JavaScript（对齐旧 IWA；报装表单 / 流程图 JS 桥按方法名调用）
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# XPopup（对齐旧 IWA / WMService）
-dontwarn com.lxj.xpopup.widget.**
-keep class com.lxj.xpopup.widget.** { *; }
-keep class com.lxj.xpopup.** { *; }
-dontwarn com.lxj.xpopup.**
