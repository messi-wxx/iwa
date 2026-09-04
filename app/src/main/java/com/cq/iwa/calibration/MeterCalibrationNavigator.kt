package com.cq.iwa.calibration

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.calibration.network.AddressResultDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MeterCalibrationNavigator {

    const val EXTRA_METER_CODE = "meterCode"
    const val EXTRA_LOCATION_JSON = "locationListJson"
    const val EXTRA_REQUIRE_AREA = "requireArea"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_LINK_ID = "linkId"
    const val EXTRA_IS_METER = "isMeterLocation"

    private val json = Json { ignoreUnknownKeys = true }

    fun open(context: Context): Boolean {
        openSearch(context)
        return true
    }

    fun openSearch(context: Context, meterCode: String? = null) {
        context.startActivity(
            Intent(context, QueryAddressActivity::class.java)
                .putExtra(EXTRA_METER_CODE, meterCode.orEmpty()),
        )
    }

    fun openForMeter(context: Context, meterCode: String) {
        context.startActivity(
            Intent(context, BuildingLocationActivity::class.java)
                .putExtra(EXTRA_METER_CODE, meterCode)
                .putExtra(EXTRA_REQUIRE_AREA, true),
        )
    }

    fun openBuilding(context: Context, meterCode: String, result: AddressResultDto?) {
        val intent = Intent(context, BuildingLocationActivity::class.java)
            .putExtra(EXTRA_METER_CODE, meterCode)
            .putExtra(EXTRA_REQUIRE_AREA, false)
        if (result != null) {
            intent.putExtra(EXTRA_LOCATION_JSON, json.encodeToString(result))
        }
        context.startActivity(intent)
    }

    fun decodeAddress(raw: String?): AddressResultDto? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<AddressResultDto>(raw) }.getOrNull()
    }
}
