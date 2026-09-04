package com.cq.iwa.feature.diagnose.di

import com.cq.iwa.feature.diagnose.network.DiagnoseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnoseNetworkModule {

    @Provides
    @Singleton
    fun provideDiagnoseApi(retrofit: Retrofit): DiagnoseApi =
        retrofit.create(DiagnoseApi::class.java)
}
