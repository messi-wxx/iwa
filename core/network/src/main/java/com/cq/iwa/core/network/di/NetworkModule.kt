package com.cq.iwa.core.network.di

import com.cq.iwa.core.network.BuildConfig
import com.cq.iwa.core.network.api.PortalApi
import com.cq.iwa.core.network.interceptor.HttpDebugInterceptor
import com.cq.iwa.core.network.interceptor.HttpErrorLogInterceptor
import com.cq.iwa.core.network.interceptor.OfflineInterceptor
import com.cq.iwa.core.network.interceptor.TokenInterceptor
import com.cq.iwa.core.network.interceptor.TokenRefreshAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 60L
    private val jsonMediaType = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @Named("plainOkHttp")
    fun providePlainOkHttpClient(
        loggingInterceptor: HttpDebugInterceptor,
        errorLogInterceptor: HttpErrorLogInterceptor,
        offlineInterceptor: OfflineInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(offlineInterceptor)
        .addInterceptor(errorLogInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("authOkHttp")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpDebugInterceptor,
        errorLogInterceptor: HttpErrorLogInterceptor,
        tokenInterceptor: TokenInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
        offlineInterceptor: OfflineInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .authenticator(tokenRefreshAuthenticator)
        .addInterceptor(offlineInterceptor)
        .addInterceptor(tokenInterceptor)
        .addInterceptor(errorLogInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("plainRetrofit")
    fun providePlainRetrofit(
        @Named("plainOkHttp") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()

    @Provides
    @Singleton
    @Named("authRetrofit")
    fun provideAuthRetrofit(
        @Named("authOkHttp") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()

    @Provides
    @Singleton
    @Named("portalPlainApi")
    fun providePortalPlainApi(@Named("plainRetrofit") retrofit: Retrofit): PortalApi =
        retrofit.create(PortalApi::class.java)

    @Provides
    @Singleton
    fun providePortalApi(@Named("authRetrofit") retrofit: Retrofit): PortalApi =
        retrofit.create(PortalApi::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(@Named("authOkHttp") okHttpClient: OkHttpClient): OkHttpClient =
        okHttpClient

    @Provides
    @Singleton
    fun provideRetrofit(@Named("authRetrofit") retrofit: Retrofit): Retrofit = retrofit
}
