package com.cq.iwa.feature.installation.di

import com.cq.iwa.feature.installation.network.InstApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InstNetworkModule {

    @Provides
    @Singleton
    fun provideInstApi(retrofit: Retrofit): InstApi = retrofit.create(InstApi::class.java)
}
