package com.cq.iwa.feature.diagnose.protocol

/**
 * 诊断蓝牙命令，字节与老 WMService 的 CNB_comand / NB_comand / Comand 一致。
 */
object DiagnoseCommands {

    fun getWaterAmount(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x14,
        payload = byteArrayOf(
            0x52, 0x45, 0x41, 0x44, 0x2C, 0x2C, 0x35, 0x0D, 0x0A,
        ),
    )

    fun uploadWaterData(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x14,
        payload = byteArrayOf(
            0x43, 0x4F, 0x4D, 0x4D, 0x2C, 0x2C, 0x31, 0x0D, 0x0A,
        ),
    )

    /** 普通 NB：READ,,0 */
    fun readCommonNbPara(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x14,
        payload = byteArrayOf(
            0x52, 0x45, 0x41, 0x44, 0x2C, 0x2C, 0x30, 0x0D, 0x0A,
        ),
    )

    /** 大连 NB / 有线：READ,,A */
    fun readDalianNbPara(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x14,
        payload = byteArrayOf(
            0x52, 0x45, 0x41, 0x44, 0x2C, 0x2C, 0x41, 0x0D, 0x0A,
        ),
    )

    fun openValve(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x16,
        payload = byteArrayOf(
            0x43, 0x4F, 0x4D, 0x4D, 0x2C, 0x2C, 0x34, 0x2C, 0x4F, 0x0D, 0x0A,
        ),
    )

    fun closeValve(): ByteArray = asciiFrame(
        size = 30,
        lenByte = 0x16,
        payload = byteArrayOf(
            0x43, 0x4F, 0x4D, 0x4D, 0x2C, 0x2C, 0x34, 0x2C, 0x43, 0x0D, 0x0A,
        ),
    )

    fun modifyWaterData(waterData: Int): ByteArray {
        val hex = DiagnoseHex.toHex(waterData).uppercase().padStart(8, '0')
        val digits = hex.toByteArray()
        val send = ByteArray(34)
        fillHeader(send, 0x1D)
        send[16] = 0x57
        send[17] = 0x52
        send[18] = 0x49
        send[19] = 0x54
        send[20] = 0x45
        send[21] = 0x2C
        send[22] = 0x34
        send[23] = 0x2C
        System.arraycopy(digits, 0, send, 24, 8)
        send[32] = 0x0D
        send[33] = 0x0A
        return send
    }

    fun parseWaterAmount(received: ByteArray): String? {
        if (received.size < 12) return null
        if (received[0] != 0x0D.toByte() || received[1] != 0x0A.toByte()) return null
        if (received[2] != 0x35.toByte() && received[2] != 0x34.toByte()) return null
        if (received[3] != 0x2C.toByte()) return null
        val nibbles = ByteArray(8)
        for (i in 0 until 8) {
            val b = received[i + 4]
            when {
                b in 0x30..0x39 -> nibbles[i] = (b - 0x30).toByte()
                b in 0x41..0x46 -> nibbles[i] = (b - 0x37).toByte()
            }
        }
        val amount = (nibbles[0].toLong() shl 28) or
            (nibbles[1].toLong() shl 24) or
            (nibbles[2].toLong() shl 20) or
            (nibbles[3].toLong() shl 16) or
            (nibbles[4].toLong() shl 12) or
            (nibbles[5].toLong() shl 8) or
            (nibbles[6].toLong() shl 4) or
            (nibbles[7].toLong())
        return amount.toString()
    }

    fun parseUploadFlag(received: ByteArray): String? {
        if (received.size < 4) return null
        return if (
            received[0] == 0x0D.toByte() &&
            received[1] == 0x0A.toByte() &&
            received[2] == 0x4F.toByte() &&
            received[3] == 0x4B.toByte()
        ) {
            "OK"
        } else {
            null
        }
    }

    fun parseDalianParaRaw(received: ByteArray): String? {
        val text = String(received)
        return if (text.isBlank()) null else text.replace("\r\n", "")
    }

    fun parseCommonNbPara(received: ByteArray, parameter: NbParameter): NbParameter? {
        val hexStr = String(received)
        if (hexStr.isEmpty()) return null
        val a = hexStr.replace("\r\n\r\n", "#")
        val b = a.replace("\r\n", "")
        if (!b.contains("#")) {
            if (b.contains(",")) {
                when (b.split(",")[1]) {
                    "0" -> parameter.valveSate = "关"
                    "1" -> parameter.valveSate = "开"
                }
            }
            return parameter
        }
        val result = b.split("#")
        for (i in result.indices) {
            val str = result[i]
            if (str.isEmpty()) continue
            if (str.contains(",")) {
                val temp = str.split(",")
                when (temp[0]) {
                    "1" -> {
                        parameter.meterCode = temp.getOrElse(1) { "" }
                        parameter.reportingPeriod = temp.getOrElse(2) { "" }
                        parameter.ip = temp.getOrElse(3) { "" }
                        parameter.port = temp.getOrElse(4) { "" }
                        parameter.agreementNo = temp.getOrElse(5) { "" }
                    }
                    "2" -> parameter.workingParameters =
                        "${temp.getOrElse(1) { "" }}--${temp.getOrElse(2) { "" }}"
                    "5" -> {
                        val hexQty = temp.getOrNull(1).orEmpty()
                        if (hexQty.isNotEmpty()) {
                            parameter.waterQty = "${hexQty.toInt(16)}L"
                        }
                    }
                    "8" -> parameter.date = temp.getOrElse(1) { "" }
                    "9" -> when (temp.getOrElse(1) { "" }) {
                        "0" -> parameter.valveSate = "关"
                        "1" -> parameter.valveSate = "开"
                    }
                }
            } else {
                when {
                    str.contains("IMEI") -> parameter.imei = str.substringAfter(":", "")
                    str.contains("IMSI") -> parameter.imsi = str.substringAfter(":", "")
                    i < 3 -> parameter.sensorNo = str
                    else -> parameter.nbNo = str
                }
            }
        }
        return parameter
    }

    private fun asciiFrame(size: Int, lenByte: Int, payload: ByteArray): ByteArray {
        val send = ByteArray(size)
        fillHeader(send, lenByte)
        System.arraycopy(payload, 0, send, 16, payload.size)
        return send
    }

    private fun fillHeader(send: ByteArray, lenByte: Int) {
        send[0] = 0x2D
        send[1] = lenByte.toByte()
        send[2] = 0x00
        send[3] = lenByte.toByte()
        send[4] = 0x00
        send[5] = 0x80.toByte()
        send[6] = 0x25
        send[7] = 0x00
        send[8] = 0x13
        send[9] = 0xDC.toByte()
        send[10] = 0x05
        send[11] = 0x14
        send[12] = 0x32
        send[13] = 0x00
        send[14] = 0x0D
        send[15] = 0x0A
    }
}
