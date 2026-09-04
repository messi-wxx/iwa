package com.cq.iwa.feature.calibration.ui

data class PlaceSearchItemUi(
    val text: String,
)

data class QueryAddressUi(
    val keyword: String,
    val items: List<PlaceSearchItemUi>,
    val empty: Boolean,
    val loadingMore: Boolean,
    val noMore: Boolean,
)

data class BuildingNodeUi(
    val id: Long,
    val name: String,
    val remark: String,
    val selected: Boolean,
)

data class BuildingLocationUi(
    val nodes: List<BuildingNodeUi>,
    val photos: List<String>,
    val remark: String,
    val lat: Double?,
    val lng: Double?,
    val selectedId: Long,
    val selectedIsMeter: Boolean,
    val selectedName: String,
    val pathText: String,
    val hasPlace: Boolean,
)
