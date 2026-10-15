package com.cq.iwa.feature.replacemeter.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import com.cq.iwa.feature.readmeter.network.FlexibleStringSerializer

@Serializable
data class ReplaceBookDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,
    val taskName: String = "",
    val lastUpdateTime: String? = null,
    val createTime: String? = null,
    val taskType: Int = 2,
)

@Serializable
data class ReplaceMeterDto(
    val id: Int = 0,
    @Serializable(with = FlexibleStringSerializer::class)
    val taskId: String? = null,
    val clientCode: String? = null,
    val customerCode: String? = null,
    val address: String? = null,
    val caliber: Int? = null,
    val oldMeterCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val oldReading: String? = null,
    val newMeterCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val newReading: String? = null,
    val replaceName: String? = null,
    val sort: Int = 0,
    val isReplace: Int = 1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @Serializable(with = FlexibleStringSerializer::class)
    val replaceRYFlux: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val extInfo: String? = null,
    val installType: String? = null,
    val verifyOrg: String? = null,
    val verifyDate: String? = null,
    val verifyExpireDate: String? = null,
)

@Serializable
data class ReplaceModelDto(
    val id: Int,
    val taskId: String? = null,
    val oldReading: Double? = null,
    val oldPicList: List<String> = emptyList(),
    val newMeterCode: String? = null,
    val newReading: Double? = null,
    val newCaliber: Int? = null,
    val newPicList: List<String> = emptyList(),
    val installType: String? = null,
    val verifyOrg: String? = null,
    @Serializable(with = BlankAsJsonNullSerializer::class)
    val verifyDate: String = "",
    @Serializable(with = BlankAsJsonNullSerializer::class)
    val verifyExpireDate: String = "",
)

/** 空日期输出 JSON null。字段保持非空 String，避免 explicitNulls=false 时把 null 省略掉。 */
internal object BlankAsJsonNullSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BlankAsJsonNull", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        if (value.isBlank()) encoder.encodeNull() else encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return if (element is JsonNull || element !is JsonPrimitive) "" else element.content
    }
}

@Serializable
data class AttachmentDto(
    val attachmentId: String = "",
    val seq: Int = 0,
)

@Serializable
data class ReplaceLocationDto(
    val linkId: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)
