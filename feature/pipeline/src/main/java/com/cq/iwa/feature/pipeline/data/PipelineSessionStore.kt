package com.cq.iwa.feature.pipeline.data

import com.cq.iwa.feature.pipeline.network.PipelineFollowDeviceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineSessionStore @Inject constructor() {
    var webSocketUrl: String = ""
    var followList: List<PipelineFollowDeviceDto> = emptyList()
    var currentSiteId: Int = -1
    var currentIotId: String? = null
    var currentSiteType: Int = -1
}
