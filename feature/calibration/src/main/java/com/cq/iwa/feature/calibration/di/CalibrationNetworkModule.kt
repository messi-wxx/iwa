package com.cq.iwa.feature.calibration.di

import com.cq.iwa.feature.calibration.network.CalibrationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalibrationNetworkModule {

    @Provides
    @Singleton
    fun provideCalibrationApi(retrofit: Retrofit): CalibrationApi =
        retrofit.create(CalibrationApi::class.java)
}
