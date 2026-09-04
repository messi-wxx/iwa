package com.cq.iwa.core.ui.di

import com.cq.iwa.core.monitor.NetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoreUiEntryPoint {
    fun networkMonitor(): NetworkMonitor
}
