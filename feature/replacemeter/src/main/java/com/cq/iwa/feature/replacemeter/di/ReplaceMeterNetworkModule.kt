package com.cq.iwa.feature.replacemeter.di

import com.cq.iwa.feature.replacemeter.network.ReplaceMeterApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReplaceMeterNetworkModule {

    @Provides
    @Singleton
    fun provideReplaceMeterApi(retrofit: Retrofit): ReplaceMeterApi =
        retrofit.create(ReplaceMeterApi::class.java)
}
