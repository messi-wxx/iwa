pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://developer.hihonor.com/repo/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "iwa"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:logger")
include(":core:storage")
include(":core:image")
include(":core:permission")
include(":core:media")
include(":core:dialog")
include(":core:monitor")
include(":feature:auth")
include(":feature:readmeter")
include(":feature:replacemeter")
include(":feature:calibration")
include(":feature:urgepayment")
include(":feature:diagnose")
include(":feature:sceneservice")
include(":feature:pipeline")
include(":feature:installation")
