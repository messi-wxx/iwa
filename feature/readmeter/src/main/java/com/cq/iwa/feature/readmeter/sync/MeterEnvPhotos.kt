package com.cq.iwa.feature.readmeter.sync

import com.cq.iwa.core.database.entity.ReadMeterEntity

internal fun ReadMeterEntity.usableEnvPhotos(): List<String> =
    envPhotos.filter { it.isNotBlank() && it != "button" }

internal fun ReadMeterEntity.hasEnvPhotos(): Boolean = usableEnvPhotos().isNotEmpty()
