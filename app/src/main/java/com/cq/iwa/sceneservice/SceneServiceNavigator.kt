package com.cq.iwa.sceneservice

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.sceneservice.network.SceneBookDto
import com.cq.iwa.feature.sceneservice.network.SceneDeviceInfoDto
import com.cq.iwa.feature.sceneservice.network.SceneJson
import com.cq.iwa.feature.sceneservice.network.ScenePartIdsDescDto
import com.cq.iwa.feature.sceneservice.network.SceneProductDefineDto
import com.cq.iwa.feature.sceneservice.network.SceneProductDto
import com.cq.iwa.feature.sceneservice.network.SceneQueryResultDto

object SceneServiceNavigator {

    const val EXTRA_QUERY = "queryResultJson"
    const val EXTRA_DEVICE = "deviceInfoJson"
    const val EXTRA_PRODUCT = "productJson"
    const val EXTRA_PARTS = "partsJson"
    const val EXTRA_DEFINE = "defineJson"
    const val EXTRA_PART_CODES = "partCodes"
    const val EXTRA_PRODUCT_ID = "productId"
    const val EXTRA_DEVICE_ID = "deviceId"
    const val EXTRA_DEVICE_ID_STR = "deviceIdStr"
    const val EXTRA_FLAG = "flag"
    const val EXTRA_REPLACE_TYPE = "replaceDeviceType"
    const val EXTRA_BOOKS = "booksJson"
    const val EXTRA_BOOK = "bookJson"
    const val EXTRA_PART = "partJson"
    const val REQUEST_CUSTOMER = 1
    const val REQUEST_REPLACE = 2
    const val REQUEST_COMPONENT = 3
    const val REQUEST_REGISTER = 4
    const val REQUEST_BOOK = 5
    const val RESULT_CUSTOMER = 1
    const val RESULT_COMPONENT = 1
    const val RESULT_REGISTER = 2

    fun open(context: Context) {
        context.startActivity(Intent(context, SceneQueryActivity::class.java))
    }

    fun openFunctions(context: Context, result: SceneQueryResultDto) {
        context.startActivity(
            Intent(context, SceneFunctionActivity::class.java)
                .putExtra(EXTRA_QUERY, SceneJson.encode(result)),
        )
    }

    fun openCustomer(context: Context) {
        if (context is android.app.Activity) {
            context.startActivityForResult(
                Intent(context, SceneCustomerActivity::class.java),
                REQUEST_CUSTOMER,
            )
        }
    }

    fun openSingleRead(context: Context, deviceId: Int, deviceInfo: SceneDeviceInfoDto? = null, replaceType: Int = 0) {
        val intent = Intent(context, SceneSingleReadActivity::class.java)
            .putExtra(EXTRA_DEVICE_ID, deviceId)
            .putExtra(EXTRA_REPLACE_TYPE, replaceType)
        if (deviceInfo != null) intent.putExtra(EXTRA_DEVICE, SceneJson.encode(deviceInfo))
        context.startActivity(intent)
    }

    fun openMeterType(context: Context, deviceId: Int) {
        context.startActivity(
            Intent(context, SceneMeterTypeActivity::class.java)
                .putExtra(EXTRA_DEVICE_ID, deviceId),
        )
    }

    fun openMeterChange(context: Context, deviceInfo: SceneDeviceInfoDto, replaceType: Int) {
        if (context is android.app.Activity) {
            context.startActivityForResult(
                Intent(context, SceneMeterChangeActivity::class.java)
                    .putExtra(EXTRA_DEVICE, SceneJson.encode(deviceInfo))
                    .putExtra(EXTRA_REPLACE_TYPE, replaceType),
                REQUEST_REPLACE,
            )
        }
    }

    fun openProductDetail(context: Context, product: SceneProductDto) {
        context.startActivity(
            Intent(context, SceneProductDetailActivity::class.java)
                .putExtra(EXTRA_PRODUCT, SceneJson.encode(product)),
        )
    }

    fun openPartList(context: Context, parts: List<ScenePartIdsDescDto>) {
        context.startActivity(
            Intent(context, ScenePartListActivity::class.java)
                .putExtra(EXTRA_PARTS, SceneJson.encode(parts)),
        )
    }

    fun openInputICode(context: Context, flag: Int, deviceId: String?) {
        context.startActivity(
            Intent(context, SceneInputICodeActivity::class.java)
                .putExtra(EXTRA_FLAG, flag)
                .putExtra(EXTRA_DEVICE_ID_STR, deviceId),
        )
    }

    fun openReport(context: Context, id: String?) {
        context.startActivity(
            Intent(context, SceneReportActivity::class.java)
                .putExtra(EXTRA_PRODUCT_ID, id),
        )
    }

    fun openUpdateComponent(context: Context, id: String?) {
        context.startActivity(
            Intent(context, SceneUpdateComponentActivity::class.java)
                .putExtra(EXTRA_PRODUCT_ID, id),
        )
    }

    fun openInputComponent(activity: android.app.Activity) {
        activity.startActivityForResult(
            Intent(activity, SceneInputComponentActivity::class.java),
            REQUEST_COMPONENT,
        )
    }

    fun openRegister(
        activity: android.app.Activity,
        define: SceneProductDefineDto,
        partCodes: List<String>,
        productId: String,
    ) {
        activity.startActivityForResult(
            Intent(activity, SceneProductRegisterActivity::class.java)
                .putExtra(EXTRA_DEFINE, SceneJson.encode(define))
                .putStringArrayListExtra(EXTRA_PART_CODES, ArrayList(partCodes))
                .putExtra(EXTRA_PRODUCT_ID, productId),
            REQUEST_REGISTER,
        )
    }

    fun openBookChoice(activity: android.app.Activity, books: List<SceneBookDto>) {
        activity.startActivityForResult(
            Intent(activity, SceneBookChoiceActivity::class.java)
                .putExtra(EXTRA_BOOKS, SceneJson.encode(books)),
            REQUEST_BOOK,
        )
    }

    fun queryResult(intent: Intent?): SceneQueryResultDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_QUERY))

    fun deviceInfo(intent: Intent?): SceneDeviceInfoDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_DEVICE))

    fun product(intent: Intent?): SceneProductDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_PRODUCT))

    fun parts(intent: Intent?): List<ScenePartIdsDescDto> =
        SceneJson.decode(intent?.getStringExtra(EXTRA_PARTS)) ?: emptyList()

    fun define(intent: Intent?): SceneProductDefineDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_DEFINE))

    fun books(intent: Intent?): List<SceneBookDto> =
        SceneJson.decode(intent?.getStringExtra(EXTRA_BOOKS)) ?: emptyList()

    fun book(intent: Intent?): SceneBookDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_BOOK))

    fun part(intent: Intent?): ScenePartIdsDescDto? =
        SceneJson.decode(intent?.getStringExtra(EXTRA_PART))
}

fun normalizeSceneScanCode(raw: String): String {
    var code = raw.replace(Regex("[\\x01-\\x1F\\x7F]"), "").trim()
    if (code.contains("zh")) code = code.replace("zh", "")
    return code
}
