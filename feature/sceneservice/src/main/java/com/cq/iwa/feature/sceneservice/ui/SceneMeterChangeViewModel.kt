package com.cq.iwa.feature.sceneservice.ui

import androidx.lifecycle.viewModelScope
import com.cq.iwa.core.common.di.IoDispatcher
import com.cq.iwa.core.common.model.ApiResult
import com.cq.iwa.core.ui.base.BaseViewModel
import com.cq.iwa.feature.sceneservice.data.SceneServiceRepository
import com.cq.iwa.feature.sceneservice.network.SceneBookDto
import com.cq.iwa.feature.sceneservice.network.SceneCaliberExtDto
import com.cq.iwa.feature.sceneservice.network.SceneDeviceInfoDto
import com.cq.iwa.feature.sceneservice.network.SceneDictOptionDto
import com.cq.iwa.feature.sceneservice.network.SceneNameplateOptionDto
import com.cq.iwa.feature.sceneservice.network.SceneReadViewModelsDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SceneMeterChangeForm(
    val replaceTypeLabel: String = "",
    val replaceReason: String = "",
    val deviceName: String = "",
    val deviceNumber: String = "",
    val identifyNumber: String = "",
    val nameplate: String = "",
    val tagText: String = "",
    val remark: String = "",
    val conveyTypeLabel: String = "",
    val valveTypeLabel: String = "",
    val valveNumber: String = "",
    val valveStateLabel: String = "",
    val useTypeLabel: String = "",
    val prepayLabel: String = "",
    val businessNumber: String = "",
    val steelStampNumber: String = "",
    val caliberLabel: String = "",
    val warningFlux: String = "",
    val maxHourFlux: String = "",
    val initFlux: String = "",
    val maxFlux: String = "",
    val installWayLabel: String = "",
    val ratio: String = "",
    val commonFlux: String = "",
    val bookName: String = "",
)

@HiltViewModel
class SceneMeterChangeViewModel @Inject constructor(
    private val repository: SceneServiceRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(ioDispatcher) {

    private val _form = MutableStateFlow(SceneMeterChangeForm())
    val form: StateFlow<SceneMeterChangeForm> = _form.asStateFlow()

    var onSaved: (() -> Unit)? = null
    var onPickBook: ((List<SceneBookDto>) -> Unit)? = null
    var onPickDict: ((String, List<SceneDictOptionDto>) -> Unit)? = null
    var onPickNameplate: ((List<SceneNameplateOptionDto>) -> Unit)? = null

    private var replaceDeviceType: Int = 1
    private var deviceInfo: SceneDeviceInfoDto? = null
    private var tagIds: MutableList<Int> = mutableListOf()
    private var nameplates: List<SceneNameplateOptionDto>? = null
    private var deviceTags: List<SceneDictOptionDto>? = null
    private var valveTypes: List<SceneDictOptionDto>? = null
    private var valveStates: List<SceneDictOptionDto>? = null
    private var useTypes: List<SceneDictOptionDto>? = null
    private var isPrePays: List<SceneDictOptionDto>? = null
    private var calibers: List<SceneDictOptionDto>? = null
    private var installWays: List<SceneDictOptionDto>? = null
    private var replaceTypes: List<SceneDictOptionDto>? = null
    private var conveyTypes: List<SceneDictOptionDto>? = null
    private var bookTree: List<SceneBookDto>? = null

    fun bind(info: SceneDeviceInfoDto, replaceDeviceType: Int) {
        this.replaceDeviceType = replaceDeviceType
        deviceInfo = info
        val device = info.deviceViewModels
        val valve = info.valveViewModels
        val water = info.waterViewModels
        val daily = info.waterDailyViewModels
        val convey = info.conveyViewModels
        tagIds = device?.tagId.orEmpty().toMutableList()
        _form.value = SceneMeterChangeForm(
            deviceName = device?.deviceName.orEmpty(),
            deviceNumber = device?.deviceCode.orEmpty(),
            identifyNumber = device?.identityCode.orEmpty(),
            nameplate = device?.nameplate.orEmpty(),
            tagText = device?.tagIdDesc.orEmpty().filter { it.isNotBlank() }.joinToString("、"),
            remark = device?.remark.orEmpty(),
            conveyTypeLabel = convey?.conveyTypeDesc.orEmpty(),
            valveTypeLabel = valve?.valveTypeDesc.orEmpty(),
            valveNumber = valve?.valveCode.orEmpty(),
            valveStateLabel = valve?.valveDesc.orEmpty(),
            useTypeLabel = water?.usageTypeDesc.orEmpty(),
            prepayLabel = water?.isPrepayDesc.orEmpty(),
            businessNumber = water?.businessCode.orEmpty(),
            steelStampNumber = water?.stampedCode.orEmpty(),
            caliberLabel = water?.caliber?.toString().orEmpty(),
            warningFlux = daily?.alertHourlyFlux.orEmpty().ifBlank {
                water?.alertHourlyFlux?.toString().orEmpty()
            },
            maxHourFlux = daily?.maxHourlyFlux.orEmpty().ifBlank {
                water?.maxHourlyFlux?.toString().orEmpty()
            },
            initFlux = water?.initialWater?.toString().orEmpty(),
            maxFlux = water?.maxWater?.toString().orEmpty(),
            installWayLabel = water?.directionDesc.orEmpty(),
            ratio = water?.ratio?.toString().orEmpty(),
            commonFlux = water?.q3?.toString().orEmpty(),
        )
        loadBooks(silent = true)
    }

    fun update(transform: (SceneMeterChangeForm) -> SceneMeterChangeForm) {
        _form.value = transform(_form.value)
        val next = _form.value
        val info = deviceInfo ?: return
        val valve = info.valveViewModels
        if (valve?.valveType == 1) {
            deviceInfo = info.copy(
                valveViewModels = valve.copy(valveCode = next.deviceNumber),
            )
            if (next.valveNumber != next.deviceNumber) {
                _form.value = next.copy(valveNumber = next.deviceNumber)
            }
        }
    }

    fun pickReplaceType() = pickDict("ReplaceType", replaceTypes) { replaceTypes = it }

    fun pickNameplate() {
        val cached = nameplates
        if (cached != null) {
            onPickNameplate?.invoke(cached)
            return
        }
        loadList(block = { repository.getDeviceNameplate() }) { list ->
            nameplates = list
            onPickNameplate?.invoke(list)
        }
    }

    fun pickTag() = pickDict("Tag", deviceTags) { deviceTags = it }

    fun pickConveyType() = pickDict("ConveyType", conveyTypes) { conveyTypes = it }

    fun pickValveType() = pickDict("ValveType", valveTypes) { valveTypes = it }

    fun pickValveState() = pickDict("Valve", valveStates) { valveStates = it }

    fun pickUseType() = pickDict("UsageType", useTypes) { useTypes = it }

    fun pickPrepay() = pickDict("YesOrNo", isPrePays) { isPrePays = it }

    fun pickCaliber() {
        val cached = calibers
        if (cached != null) {
            onPickDict?.invoke("Caliber", cached)
            return
        }
        loadList(block = { repository.getCaliber() }) { list ->
            calibers = list
            onPickDict?.invoke("Caliber", list)
        }
    }

    fun pickInstallWay() = pickDict("Direction", installWays) { installWays = it }

    fun pickBook() {
        val cached = bookTree
        if (cached != null) {
            if (cached.isEmpty()) showToast("表册为空") else onPickBook?.invoke(cached)
            return
        }
        loadBooks(silent = false)
    }

    fun applyNameplate(option: SceneNameplateOptionDto) {
        val info = deviceInfo ?: return
        val device = info.deviceViewModels ?: return
        deviceInfo = info.copy(deviceViewModels = device.copy(nameplate = option.label))
        _form.value = _form.value.copy(nameplate = option.label.orEmpty())
    }

    fun applyDict(kind: String, option: SceneDictOptionDto) {
        val info = deviceInfo ?: return
        when (kind) {
            "ReplaceType" -> {
                deviceInfo = info.copy(replaceType = option.value)
                _form.value = _form.value.copy(replaceTypeLabel = option.label.orEmpty())
            }
            "Tag" -> applyTag(option)
            "ConveyType" -> {
                val convey = info.conveyViewModels ?: return
                deviceInfo = info.copy(
                    conveyViewModels = convey.copy(
                        conveyType = option.value,
                        conveyTypeDesc = option.label,
                    ),
                )
                _form.value = _form.value.copy(conveyTypeLabel = option.label.orEmpty())
            }
            "ValveType" -> {
                val valve = info.valveViewModels ?: return
                val number = if (option.value == 1) _form.value.deviceNumber else _form.value.valveNumber
                deviceInfo = info.copy(
                    valveViewModels = valve.copy(valveType = option.value, valveCode = number),
                )
                _form.value = _form.value.copy(
                    valveTypeLabel = option.label.orEmpty(),
                    valveNumber = number,
                )
            }
            "Valve" -> {
                val valve = info.valveViewModels ?: return
                deviceInfo = info.copy(valveViewModels = valve.copy(valve = option.value))
                _form.value = _form.value.copy(valveStateLabel = option.label.orEmpty())
            }
            "UsageType" -> {
                val water = info.waterViewModels ?: return
                deviceInfo = info.copy(
                    waterViewModels = water.copy(
                        usageType = option.value,
                        usageTypeDesc = option.label,
                    ),
                )
                _form.value = _form.value.copy(useTypeLabel = option.label.orEmpty())
            }
            "YesOrNo" -> {
                val water = info.waterViewModels ?: return
                deviceInfo = info.copy(
                    waterViewModels = water.copy(
                        isPrepay = option.value,
                        isPrepayDesc = option.label,
                    ),
                )
                _form.value = _form.value.copy(prepayLabel = option.label.orEmpty())
            }
            "Caliber" -> applyCaliber(option)
            "Direction" -> {
                val water = info.waterViewModels ?: return
                deviceInfo = info.copy(
                    waterViewModels = water.copy(
                        direction = option.value,
                        directionDesc = option.label,
                    ),
                )
                _form.value = _form.value.copy(installWayLabel = option.label.orEmpty())
            }
        }
    }

    fun applyBook(book: SceneBookDto) {
        val info = deviceInfo ?: return
        val read = info.readViewModels ?: SceneReadViewModelsDto()
        deviceInfo = info.copy(readViewModels = read.copy(bookWaterId = book.value))
        _form.value = _form.value.copy(bookName = book.data.orEmpty())
    }

    fun submit() {
        val form = _form.value
        val info = deviceInfo ?: return
        val device = info.deviceViewModels
        val valve = info.valveViewModels
        val water = info.waterViewModels
        if (device == null || valve == null || water == null) {
            showToast("无法继续,设备不存在")
            return
        }
        if (form.replaceTypeLabel.isBlank()) {
            showToast("更换类型不能为空")
            return
        }
        if (form.replaceReason.isBlank()) {
            showToast("更换理由不能为空")
            return
        }
        if (form.deviceNumber.isBlank()) {
            showToast("设备编号不能为空")
            return
        }
        if (form.nameplate.isBlank()) {
            showToast("设备铭牌不能为空")
            return
        }
        if (replaceDeviceType != 1 && form.conveyTypeLabel.isBlank()) {
            showToast("传输类型不能为空")
            return
        }
        if (form.valveTypeLabel.isBlank()) {
            showToast("阀门类型不能为空")
            return
        }
        if (form.valveNumber.isBlank()) {
            showToast("阀门编号不能为空")
            return
        }
        if (form.valveStateLabel.isBlank()) {
            showToast("阀门状态不能为空")
            return
        }
        if (form.useTypeLabel.isBlank()) {
            showToast("使用类型不能为空")
            return
        }
        if (form.prepayLabel.isBlank()) {
            showToast("是否预付费不能为空")
            return
        }
        if (form.caliberLabel.isBlank()) {
            showToast("口径不能为空")
            return
        }
        if (form.warningFlux.isBlank()) {
            showToast("警示时流量不能为空")
            return
        }
        if (!isSceneWaterReadDecimal(form.warningFlux)) {
            showToast("警示时流量最多七位整数并且最多三位小数")
            return
        }
        if (form.maxHourFlux.isBlank()) {
            showToast("最大时流量不能为空")
            return
        }
        if (form.initFlux.isBlank()) {
            showToast("初始水量不能为空")
            return
        }
        if (!isSceneWaterReadDecimal(form.initFlux)) {
            showToast("初始水量最多七位整数并且最多三位小数")
            return
        }
        if (form.maxFlux.isBlank()) {
            showToast("最大水量读数不能为空")
            return
        }
        if (!isSceneWaterReadDecimal(form.maxFlux)) {
            showToast("最大水量最多七位整数并且最多三位小数")
            return
        }
        if (form.installWayLabel.isBlank()) {
            showToast("安装方式不能为空")
            return
        }
        if (form.ratio.isBlank()) {
            showToast("量程比不能为空")
            return
        }
        if (form.commonFlux.isBlank()) {
            showToast("常用流量不能为空")
            return
        }
        if (!isSceneWaterReadDecimal(form.maxFlux)) {
            showToast("常用流量最多七位整数并且最多三位小数")
            return
        }
        if (form.bookName.isBlank()) {
            showToast("水表抄读册不能为空")
            return
        }
        val body = info.copy(
            reason = form.replaceReason,
            waterDailyViewModels = null,
            deviceViewModels = device.copy(
                deviceName = form.deviceName,
                deviceCode = form.deviceNumber,
                identityCode = form.identifyNumber,
                remark = form.remark,
            ),
            valveViewModels = valve.copy(valveCode = form.valveNumber),
            waterViewModels = water.copy(
                businessCode = form.businessNumber,
                stampedCode = form.steelStampNumber,
                alertHourlyFlux = form.warningFlux.toFloatOrNull(),
                maxHourlyFlux = form.maxHourFlux.toFloatOrNull(),
                initialWater = form.initFlux.toFloatOrNull(),
                maxWater = form.maxFlux.toFloatOrNull(),
                ratio = form.ratio.toIntOrNull(),
                q3 = form.commonFlux.toFloatOrNull(),
            ),
        )
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { repository.replaceDevice(body) }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    showToast("操作成功")
                    onSaved?.invoke()
                }
            }
        }
    }

    fun requireConveyType(): Boolean = replaceDeviceType != 1

    private fun applyTag(option: SceneDictOptionDto) {
        val info = deviceInfo ?: return
        val device = info.deviceViewModels ?: return
        val tags = deviceTags.orEmpty()
        val index = tagIds.indexOf(option.value)
        if (index < 0) tagIds.add(option.value) else tagIds.removeAt(index)
        val desc = tagIds.mapNotNull { id -> tags.firstOrNull { it.value == id }?.label }
        deviceInfo = info.copy(deviceViewModels = device.copy(tagId = tagIds.toList(), tagIdDesc = desc))
        _form.value = _form.value.copy(tagText = desc.joinToString("、"))
    }

    private fun applyCaliber(option: SceneDictOptionDto) {
        val info = deviceInfo ?: return
        val water = info.waterViewModels ?: return
        val ext: SceneCaliberExtDto? = option.data
        deviceInfo = info.copy(
            waterViewModels = water.copy(
                caliber = option.value,
                alertHourlyFlux = ext?.alertHourlyFlux,
                maxHourlyFlux = ext?.maxHourlyFlux,
            ),
        )
        _form.value = _form.value.copy(
            caliberLabel = option.label.orEmpty(),
            warningFlux = ext?.alertHourlyFlux?.toString().orEmpty(),
            maxHourFlux = ext?.maxHourlyFlux?.toString().orEmpty(),
        )
    }

    private fun pickDict(
        kind: String,
        cached: List<SceneDictOptionDto>?,
        save: (List<SceneDictOptionDto>) -> Unit,
    ) {
        if (cached != null) {
            onPickDict?.invoke(kind, cached)
            return
        }
        val code = when (kind) {
            "Tag" -> null
            else -> kind
        }
        loadList(
            block = {
                if (kind == "Tag") repository.getDeviceTag() else repository.getDictionary(code!!)
            },
        ) { list ->
            save(list)
            onPickDict?.invoke(kind, list)
        }
    }

    private fun loadBooks(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) showLoading()
            val result = withContext(ioDispatcher) { repository.getBookTree() }
            if (!silent) hideLoading()
            when (result) {
                is ApiResult.Error -> if (!silent) showToast(result.message)
                is ApiResult.Success -> {
                    bookTree = result.data
                    if (!silent) {
                        if (result.data.isEmpty()) showToast("表册为空") else onPickBook?.invoke(result.data)
                    }
                }
            }
        }
    }

    private fun <T> loadList(
        block: suspend () -> ApiResult<List<T>>,
        onOk: (List<T>) -> Unit,
    ) {
        viewModelScope.launch {
            showLoading()
            val result = withContext(ioDispatcher) { block() }
            hideLoading()
            when (result) {
                is ApiResult.Error -> showToast(result.message)
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) showToast("无数据") else onOk(result.data)
                }
            }
        }
    }
}
