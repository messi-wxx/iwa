package com.cq.iwa.feature.diagnose.protocol

data class NbParameter(
    var meterCode: String = "",
    var reportingPeriod: String = "",
    var ip: String = "",
    var port: String = "",
    var agreementNo: String = "",
    var workingParameters: String = "",
    var sensorNo: String = "",
    var waterQty: String = "",
    var date: String = "",
    var nbNo: String = "",
    var imei: String = "",
    var imsi: String = "",
    var valveSate: String = "",
)
