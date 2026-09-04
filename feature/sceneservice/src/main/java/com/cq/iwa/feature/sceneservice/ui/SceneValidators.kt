package com.cq.iwa.feature.sceneservice.ui

fun isSceneWaterRead(read: String): Boolean {
    if (read == "0") return true
    return Regex("^[1-9]\\d{0,8}$").matches(read)
}

fun isSceneWaterReadDecimal(read: String): Boolean =
    Regex("^[1-9]\\d{0,6}(\\.\\d{1,3})?$|^0(\\.\\d{1,3})?$").matches(read)
