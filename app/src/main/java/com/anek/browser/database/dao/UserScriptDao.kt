package com.anek.browser.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.anek.browser.database.entity.UserScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserScriptDao {

    @Query("SELECT * FROM user_scripts ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<UserScriptEntity>>

    @Query("SELECT * FROM user_scripts WHERE enabled = 1")
    suspend fun getEnabled(): List<UserScriptEntity>

    @Query("SELECT * FROM user_scripts WHERE id = :id")
    suspend fun getById(id: Long): UserScriptEntity?

    @Insert
    suspend fun insert(script: UserScriptEntity): Long

    @Update
    suspend fun update(script: UserScriptEntity)

    @Delete
    suspend fun delete(script: UserScriptEntity)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE user_scripts SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE user_scripts SET lastRunAt = :at WHERE id = :id")
    suspend fun touchRun(id: Long, at: Long)

    @Query("DELETE FROM user_scripts")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM user_scripts")
    suspend fun count(): Int
}
