package com.cq.iwa.feature.dma.di

import com.cq.iwa.feature.dma.BuildConfig
import com.cq.iwa.feature.dma.network.DmaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DmaNetworkModule {

    @Provides
    @Singleton
    @Named("dmaRetrofit")
    fun provideDmaRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.DMA_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDmaApi(@Named("dmaRetrofit") retrofit: Retrofit): DmaApi =
        retrofit.create(DmaApi::class.java)
}
