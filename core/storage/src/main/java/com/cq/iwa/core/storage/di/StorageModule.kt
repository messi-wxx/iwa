package com.cq.iwa.core.storage.di

import com.cq.iwa.core.network.auth.SessionStore
import com.cq.iwa.core.network.auth.TokenProvider
import com.cq.iwa.core.storage.auth.MmkvSessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: MmkvSessionStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: MmkvSessionStore): TokenProvider
}
