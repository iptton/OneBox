package com.shifenmiao.database.ai.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shifenmiao.database.ai.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {

    @Query("SELECT * FROM skill ORDER BY name ASC")
    fun observeAll(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skill ORDER BY name ASC")
    suspend fun getAll(): List<SkillEntity>

    @Query("SELECT * FROM skill WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SkillEntity)

    @Update
    suspend fun update(entity: SkillEntity)

    @Query("DELETE FROM skill WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * use_skill 命中后自增使用频次。
     */
    @Query("UPDATE skill SET use_count = use_count + 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun incrementUseCount(id: String, updatedAt: Long)

    /**
     * 归一化批量写回（use_count 超上限时全体缩放到 0–100），由调用方在事务内逐行执行。
     */
    @Query("UPDATE skill SET use_count = :useCount, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateUseCount(id: String, useCount: Double, updatedAt: Long)
}
