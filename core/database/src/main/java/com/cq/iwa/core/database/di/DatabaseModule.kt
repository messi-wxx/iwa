package com.cq.iwa.core.database.di

import android.content.Context
import androidx.room.Room
import com.cq.iwa.core.database.AppDatabase
import com.cq.iwa.core.database.dao.HistoryReadMeterDao
import com.cq.iwa.core.database.dao.MeterBookDao
import com.cq.iwa.core.database.dao.ReadMeterDao
import com.cq.iwa.core.database.dao.ReplaceMeterDao
import com.cq.iwa.core.database.dao.UserConfigDao
import com.cq.iwa.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iwa.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideUserConfigDao(database: AppDatabase): UserConfigDao = database.userConfigDao()

    @Provides
    fun provideMeterBookDao(database: AppDatabase): MeterBookDao = database.meterBookDao()

    @Provides
    fun provideReadMeterDao(database: AppDatabase): ReadMeterDao = database.readMeterDao()

    @Provides
    fun provideHistoryReadMeterDao(database: AppDatabase): HistoryReadMeterDao =
        database.historyReadMeterDao()

    @Provides
    fun provideReplaceMeterDao(database: AppDatabase): ReplaceMeterDao =
        database.replaceMeterDao()
}
