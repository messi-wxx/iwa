package com.cq.iwa.feature.sceneservice.di

import com.cq.iwa.feature.sceneservice.network.SceneApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SceneNetworkModule {

    @Provides
    @Singleton
    fun provideSceneApi(retrofit: Retrofit): SceneApi =
        retrofit.create(SceneApi::class.java)
}
