package com.cq.iwa.feature.readmeter.di

import com.cq.iwa.feature.readmeter.BuildConfig
import com.cq.iwa.feature.readmeter.network.FileApi
import com.cq.iwa.feature.readmeter.network.MeterApi
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
object ReadMeterNetworkModule {

    @Provides
    @Singleton
    fun provideMeterApi(retrofit: Retrofit): MeterApi = retrofit.create(MeterApi::class.java)

    @Provides
    @Singleton
    @Named("fileRetrofit")
    fun provideFileRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.FILE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideFileApi(@Named("fileRetrofit") retrofit: Retrofit): FileApi =
        retrofit.create(FileApi::class.java)
}
