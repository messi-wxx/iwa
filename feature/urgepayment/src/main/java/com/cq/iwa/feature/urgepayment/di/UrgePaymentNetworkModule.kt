package com.cq.iwa.feature.urgepayment.di

import com.cq.iwa.feature.urgepayment.network.UrgePaymentApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UrgePaymentNetworkModule {

    @Provides
    @Singleton
    fun provideUrgePaymentApi(retrofit: Retrofit): UrgePaymentApi =
        retrofit.create(UrgePaymentApi::class.java)
}
