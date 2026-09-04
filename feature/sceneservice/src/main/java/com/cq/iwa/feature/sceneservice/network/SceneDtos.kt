package com.cq.iwa.feature.sceneservice.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SceneQueryResultDto(
    val edcId: Int = 0,
    val epoId: String = "",
    val itWaterId: Int = 0,
    val edcDeviceInfo: SceneEdcDeviceInfoDto? = null,
    val epoProductInfo: SceneProductDto? = null,
    val itWaterMeterInfo: SceneItWaterMeterInfoDto? = null,
) {
    fun isEmpty(): Boolean = edcId == 0 && epoId.isBlank() && itWaterId == 0
}

@Serializable
data class SceneEdcDeviceInfoDto(
    val id: Int = 0,
    val deviceName: String? = null,
    val deviceCode: String? = null,
    val identityCode: String? = null,
    val nameplate: String? = null,
    val address: String? = null,
    val createTime: String? = null,
    val customerCode: String? = null,
)

@Serializable
data class SceneItWaterMeterInfoDto(
    val code: String? = null,
    val theSize: String? = null,
    val meterType: String? = null,
    val namePlate: String? = null,
    val instAddr: String? = null,
    val instDate: String? = null,
)

@Serializable
data class SceneProductDto(
    val id: String? = null,
    val code: String? = null,
    val fullCode: String? = null,
    val creator: String? = null,
    val createTime: String? = null,
    val customers: List<String>? = null,
    val partIds: List<String>? = null,
    val partIdsDesc: List<ScenePartIdsDescDto>? = null,
    val productDefineId: String? = null,
    val productDefineIdDesc: SceneProductDefineDto? = null,
    val partDefineIdDesc: ScenePartDefineDto? = null,
    val state: Int = 0,
    val currentState: Int = 0,
    val customersDesc: String? = null,
    val useForTypeDesc: String? = null,
    val currentStateDesc: String? = null,
    val stateDesc: String? = null,
    val useForType: Int = 0,
    val installState: Int = 0,
    val remark: String? = null,
)

@Serializable
data class ScenePartIdsDescDto(
    val id: String? = null,
    val code: String? = null,
    val fullCode: String? = null,
    val partDefineInfoName: String? = null,
    val partDefineInfoCode: String? = null,
    val partDefineInfoSoftwareVer: String? = null,
    val partDefineInfoId: String? = null,
    val partCategoryId: String? = null,
)

@Serializable
data class ScenePartDefineDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val softwareVer: String? = null,
    val remark: String? = null,
)

@Serializable
data class SceneProductDefineDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val partDefines: List<String>? = null,
    val techState: Int = 0,
    val propertys: List<ScenePropertyDto>? = null,
    val remark: String? = null,
    val productCategoryId: String? = null,
    val createTime: String? = null,
    val creator: String? = null,
)

@Serializable
data class ScenePropertyDto(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val dataType: String? = null,
    @SerialName("isStop") val isStop: Boolean = false,
    val controlType: Int = 0,
    val categoryType: Int = 0,
    val optionalData: List<SceneEpoOptionDto>? = null,
    @SerialName("isRequired") val isRequired: Boolean = false,
)

@Serializable
data class SceneEpoOptionDto(
    val label: String? = null,
    val value: String? = null,
)

@Serializable
data class SceneCustomerDto(
    val text: String? = null,
    val value: String? = null,
)

@Serializable
data class SceneSingleReadRequestDto(
    val id: Int,
    val water: Float,
    val displayFlux: Float? = null,
    val positiveFlux: Float? = null,
    val inversionFlux: Float? = null,
)

@Serializable
data class SceneICodeRequestDto(
    val deviceID: String,
    val fullCode: String,
)

@Serializable
data class ScenePartDefineIdsBodyDto(
    val partDefineIds: List<String>,
)

@Serializable
data class SceneReplacePartBodyDto(
    val productId: String? = null,
    val productInfo: SceneReplaceProductInfoDto? = null,
    val reason: String? = null,
)

@Serializable
data class SceneReplaceProductInfoDto(
    val code: String? = null,
    val fullCode: String? = null,
    val productDefineId: String? = null,
    val propertysList: Map<String, String>? = null,
    val partCodes: List<String>? = null,
    val remark: String? = null,
)

@Serializable
data class SceneDeviceInfoDto(
    val id: Int = 0,
    val reason: String? = null,
    val replaceType: Int = 0,
    val deviceViewModels: SceneDeviceViewModelsDto? = null,
    val conveyViewModels: SceneConveyViewModelsDto? = null,
    val pressureViewModels: ScenePressureViewModelsDto? = null,
    val temperatureViewModels: SceneTemperatureViewModelsDto? = null,
    val valveViewModels: SceneValveViewModelsDto? = null,
    val qualityViewModels: SceneQualityViewModelsDto? = null,
    val waterViewModels: SceneWaterViewModelsDto? = null,
    val waterDailyViewModels: SceneWaterDailyViewModelsDto? = null,
    val readViewModels: SceneReadViewModelsDto? = null,
    val waterRealtimeViewModels: SceneWaterRealtimeViewModelsDto? = null,
)

@Serializable
data class SceneDeviceViewModelsDto(
    val id: Int = 0,
    val newId: Int = 0,
    val deviceId: Int = 0,
    val kindId: Int = 0,
    val kindCode: String? = null,
    val kindIdDesc: String? = null,
    val conveyWay: Int = 0,
    val valveWay: Int = 0,
    val waterWay: Int = 0,
    val pressureWay: Int = 0,
    val temperatureWay: Int = 0,
    val qualityWay: Int = 0,
    val locationId: Int = 0,
    val locationIdDesc: String? = null,
    val schedule: Int = 0,
    val scheduleDesc: String? = null,
    val registry: Int = 0,
    val registryDesc: String? = null,
    val deviceName: String? = null,
    val deviceCode: String? = null,
    val identityCode: String? = null,
    val nameplate: String? = null,
    val address: String? = null,
    val remark: String? = null,
    val createTime: String? = null,
    val createByName: String? = null,
    val createByGuid: String? = null,
    val tagId: List<Int>? = null,
    val tagIdDesc: List<String>? = null,
)

@Serializable
data class SceneConveyViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
    val conveyType: Int = 0,
    val conveyTypeDesc: String? = null,
)

@Serializable
data class ScenePressureViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
    val minAlertPressure: String? = null,
    val maxAlertPressure: String? = null,
    val maxPressure: String? = null,
)

@Serializable
data class SceneTemperatureViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
)

@Serializable
data class SceneQualityViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
)

@Serializable
data class SceneValveViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
    val valveType: Int = 0,
    val valveCode: String? = null,
    val valve: Int = 0,
    val valveTime: String? = null,
    val valveMonitorTime: String? = null,
    val valveMonitor: String? = null,
    val valveTypeDesc: String? = null,
    val valveDesc: String? = null,
    val valveMonitorDesc: String? = null,
)

@Serializable
data class SceneWaterViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
    val usageType: Int = 0,
    val isPrepay: Int = 0,
    val businessCode: String? = null,
    val stampedCode: String? = null,
    val caliber: Int? = null,
    val alertHourlyFlux: Float? = null,
    val maxHourlyFlux: Float? = null,
    val initialWater: Float? = null,
    val maxWater: Float? = null,
    val direction: Int? = null,
    val ratio: Int? = null,
    val q1: Float? = null,
    val q2: Float? = null,
    val q3: Float? = null,
    val q4: Float? = null,
    val directionDesc: String? = null,
    val usageTypeDesc: String? = null,
    val isPrepayDesc: String? = null,
)

@Serializable
data class SceneWaterDailyViewModelsDto(
    val id: Int = 0,
    val deviceId: Int = 0,
    val deviceCode: String? = null,
    val alertHourlyFlux: String? = null,
    val maxHourlyFlux: String? = null,
    val water: Float? = null,
    val maxWater: Float? = null,
)

@Serializable
data class SceneReadViewModelsDto(
    val bookWaterId: Int = 0,
)

@Serializable
data class SceneWaterRealtimeViewModelsDto(
    val water: Float? = null,
    val displayFlux: Float = 0f,
    val positiveFlux: Float = 0f,
    val inversionFlux: Float = 0f,
)

@Serializable
data class SceneDictOptionDto(
    val label: String? = null,
    val value: Int = 0,
    val disabled: Boolean = false,
    val data: SceneCaliberExtDto? = null,
)

@Serializable
data class SceneNameplateOptionDto(
    val label: String? = null,
    val value: String? = null,
    val disabled: Boolean = false,
)

@Serializable
data class SceneCaliberExtDto(
    val alertHourlyFlux: Float? = null,
    val maxHourlyFlux: Float? = null,
)

@Serializable
data class SceneBookDto(
    val value: Int = 0,
    val label: String? = null,
    val data: String? = null,
    val children: List<SceneBookDto>? = null,
)
