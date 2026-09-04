package com.cq.iwa.feature.diagnose.protocol

object DiagnoseHex {

    private const val WAKEUP = "0D 0A 31 32 33 34 35 0D 0A"

    fun toHex(bytes: ByteArray, separator: String = " "): String =
        bytes.joinToString(separator) { "%02X".format(it) }

    fun toHex(value: Int): String = Integer.toHexString(value)

    fun isWakeup(bytes: ByteArray): Boolean = toHex(bytes) == WAKEUP
}
