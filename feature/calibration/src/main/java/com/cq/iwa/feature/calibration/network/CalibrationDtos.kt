package com.cq.iwa.feature.calibration.network

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDto(
    val attachmentId: String = "",
    val seq: Long = 0,
)

@Serializable
data class LocationPlaceDto(
    val ulinfoId: Long = 0,
    val linkId: Long = 0,
    val linkTable: String? = null,
    val name: String? = null,
    val meterCode: String? = null,
    val lng: Double? = null,
    val lat: Double? = null,
    val remark: String? = null,
    val creator: String? = null,
    val createTime: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)

/** 新增提交体：只带老 Gson 实际会发出的字段，避免把 null 的 name/linkTable 等一并送出。 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LocationPlaceSubmitDto(
    val ulinfoId: Long = 0,
    val linkId: Long,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val meterCode: String? = null,
    val lng: Double,
    val lat: Double,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val remark: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)

@Serializable
data class AddressResultDto(
    val meterId: Int = 0,
    val meterCode: String? = null,
    val bookLocations: List<BookLocationDto> = emptyList(),
)

@Serializable
data class BookLocationDto(
    val id: Int = 0,
    val parentId: Int = 0,
    val isEnabled: Int = 0,
    val sort: Int = 0,
    val name: String? = null,
    val remark: String? = null,
)

@Serializable
data class LocationRecordDto(
    val value: Int = 0,
    val text: String? = null,
)

@Serializable
data class PlaceSearchDto(
    val key: PlaceSearchPageDto? = null,
    val value: List<PlaceSearchItemDto> = emptyList(),
)

@Serializable
data class PlaceSearchPageDto(
    val pageSize: Int = 0,
    val currentPage: Int = 0,
    val pageTotal: Int = 0,
)

@Serializable
data class PlaceSearchItemDto(
    val value: String? = null,
    val text: String? = null,
)
