package com.cq.iwa.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.cq.iwa.core.database.entity.UserConfigEntity
import com.cq.iwa.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao : BaseDao<UserEntity> {

    @Query("SELECT * FROM user WHERE currentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM user WHERE currentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE customerCode = :customerCode AND code = :code LIMIT 1")
    suspend fun findByAccount(customerCode: String, code: String): UserEntity?

    @Query("UPDATE user SET currentUser = 0")
    suspend fun clearCurrentUserFlag()

    @Query("UPDATE user SET state = :state, password = :password WHERE code = :userCode")
    suspend fun updateStateAndPassword(userCode: String, state: Int, password: String)
}

@Dao
interface UserConfigDao : BaseDao<UserConfigEntity> {

    @Query("DELETE FROM user_config")
    suspend fun clearUserConfigs()

    @Query("SELECT * FROM user_config WHERE configName = :name LIMIT 1")
    suspend fun findByName(name: String): UserConfigEntity?

    @Query("SELECT * FROM user_config WHERE configName = :name ORDER BY seq ASC")
    suspend fun findAllByName(name: String): List<UserConfigEntity>

    @Query("UPDATE user_config SET configValue = :value WHERE configName = :name")
    suspend fun updateValue(name: String, value: String)
}
