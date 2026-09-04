package com.cq.iwa.feature.pipeline.di

import com.cq.iwa.feature.pipeline.network.PipelineApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PipelineNetworkModule {

    @Provides
    @Singleton
    fun providePipelineApi(retrofit: Retrofit): PipelineApi = retrofit.create(PipelineApi::class.java)
}
