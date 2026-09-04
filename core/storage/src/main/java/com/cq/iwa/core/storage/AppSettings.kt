package com.cq.iwa.core.storage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor() {
    var privacyAgreed by mmkvDelegate("privacy_agreed", false)
    var replaceShowWay by mmkvDelegate("updateMeterSettingWay", 2)
    var replaceInstallType by mmkvDelegate("installType", "立式")
    var replaceGroupName by mmkvDelegate("updateMeterGroupName", "")
    var readMeterGroupName by mmkvDelegate("groupName", "")
    var lastLatitude by mmkvDelegate("lat", "0")
    var lastLongitude by mmkvDelegate("lng", "0")
    var skippedUpdateVer by mmkvDelegate("skipped_update_ver", 0)
}
