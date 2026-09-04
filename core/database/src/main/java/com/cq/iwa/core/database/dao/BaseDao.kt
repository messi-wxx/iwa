package com.cq.iwa.core.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/**
 * 泛型 DAO 基础操作，具体 DAO 继承并补充查询
 */
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>): List<Long>

    @Update
    suspend fun update(entity: T): Int

    @Update
    suspend fun updateAll(entities: List<T>): Int

    @Delete
    suspend fun delete(entity: T): Int

    @Delete
    suspend fun deleteAll(entities: List<T>): Int
}
