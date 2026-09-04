package com.cq.iwa.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cq.iwa.core.database.converter.DateConverter
import com.cq.iwa.core.database.converter.StringListConverter
import com.cq.iwa.core.database.converter.StringMapConverter
import com.cq.iwa.core.database.dao.HistoryReadMeterDao
import com.cq.iwa.core.database.dao.MeterBookDao
import com.cq.iwa.core.database.dao.ReadMeterDao
import com.cq.iwa.core.database.dao.ReplaceMeterDao
import com.cq.iwa.core.database.dao.UserConfigDao
import com.cq.iwa.core.database.dao.UserDao
import com.cq.iwa.core.database.entity.HistoryReadMeterEntity
import com.cq.iwa.core.database.entity.MeterBookEntity
import com.cq.iwa.core.database.entity.ReadMeterEntity
import com.cq.iwa.core.database.entity.ReplaceMeterEntity
import com.cq.iwa.core.database.entity.UserConfigEntity
import com.cq.iwa.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        UserConfigEntity::class,
        MeterBookEntity::class,
        ReadMeterEntity::class,
        HistoryReadMeterEntity::class,
        ReplaceMeterEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(DateConverter::class, StringListConverter::class, StringMapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun meterBookDao(): MeterBookDao
    abstract fun readMeterDao(): ReadMeterDao
    abstract fun historyReadMeterDao(): HistoryReadMeterDao
    abstract fun replaceMeterDao(): ReplaceMeterDao
}
