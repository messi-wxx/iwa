package com.cq.iwa.sdk

import com.cq.iwa.core.network.interceptor.NetworkErrorReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BuglyReportModule {

    @Provides
    @Singleton
    fun provideNetworkErrorReporter(): NetworkErrorReporter =
        NetworkErrorReporter(ThirdSdk::reportNetworkError)
}
