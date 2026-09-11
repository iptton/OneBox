package com.shifenmiao.database.decision_wheel.dao

import androidx.room.*
import com.shifenmiao.database.decision_wheel.entity.WheelEntity
import com.shifenmiao.database.decision_wheel.entity.WheelWithOptions
import com.shifenmiao.database.decision_wheel.entity.WheelHistoryEntity
import com.shifenmiao.database.decision_wheel.entity.WheelOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WheelDao {

    // Wheel CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWheel(wheel: WheelEntity): Long

    @Update
    suspend fun updateWheel(wheel: WheelEntity)

    @Delete
    suspend fun deleteWheel(wheel: WheelEntity)

    /**
     * 转盘列表（含选项）。
     *
     * 必须用 @Relation：Room 只对 SQL 里出现的表做失效追踪，而抽后移除 / 全部恢复
     * 改的是 wheel_options。只查 decision_wheels 的话这些改动永远推不到界面上。
     */
    @Transaction
    @Query("SELECT * FROM decision_wheels ORDER BY lastUsedAt DESC")
    fun observeWheelsWithOptions(): Flow<List<WheelWithOptions>>

    @Query("SELECT * FROM decision_wheels WHERE id = :wheelId")
    suspend fun getWheelById(wheelId: String): WheelEntity?

    @Transaction
    @Query("SELECT * FROM decision_wheels ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecentWheelsWithOptions(limit: Int = 5): Flow<List<WheelWithOptions>>

    @Query("UPDATE decision_wheels SET lastUsedAt = :timestamp, useCount = useCount + 1 WHERE id = :wheelId")
    suspend fun updateWheelUsage(wheelId: String, timestamp: Long)

    // Option CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: WheelOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<WheelOptionEntity>)

    @Update
    suspend fun updateOption(option: WheelOptionEntity)

    @Delete
    suspend fun deleteOption(option: WheelOptionEntity)

    @Query("SELECT * FROM wheel_options WHERE wheelId = :wheelId ORDER BY position ASC")
    suspend fun getOptionsByWheelId(wheelId: String): List<WheelOptionEntity>

    @Query("SELECT * FROM wheel_options WHERE wheelId = :wheelId ORDER BY position ASC")
    fun getOptionsByWheelIdFlow(wheelId: String): Flow<List<WheelOptionEntity>>

    @Query("DELETE FROM wheel_options WHERE wheelId = :wheelId")
    suspend fun deleteOptionsByWheelId(wheelId: String)

    // 抽后移除：只翻转启用标记，不走全量重插（重插会重置 enabled）
    @Query("UPDATE wheel_options SET enabled = :enabled WHERE id = :optionId")
    suspend fun updateOptionEnabled(optionId: String, enabled: Boolean)

    @Query("UPDATE wheel_options SET enabled = 1 WHERE wheelId = :wheelId")
    suspend fun resetOptionsEnabled(wheelId: String)

    // History operations
    @Insert
    suspend fun insertHistory(history: WheelHistoryEntity): Long

    @Query("SELECT * FROM wheel_history WHERE wheelId = :wheelId ORDER BY timestamp DESC LIMIT :limit")
    fun getWheelHistory(wheelId: String, limit: Int = 20): Flow<List<WheelHistoryEntity>>

    @Query("SELECT * FROM wheel_history ORDER BY timestamp DESC LIMIT :limit")
    fun getAllHistory(limit: Int = 50): Flow<List<WheelHistoryEntity>>

    @Query("SELECT * FROM wheel_history ORDER BY timestamp DESC")
    fun observeAllHistory(): Flow<List<WheelHistoryEntity>>

    @Query("DELETE FROM wheel_history WHERE wheelId = :wheelId")
    suspend fun deleteHistoryByWheelId(wheelId: String)

    @Query("DELETE FROM wheel_history WHERE id = :historyId")
    suspend fun deleteHistoryById(historyId: Long)

    @Query("DELETE FROM wheel_history")
    suspend fun clearAllHistory()

    // Composite operations
    @Transaction
    suspend fun insertWheelWithOptions(wheel: WheelEntity, options: List<WheelOptionEntity>) {
        insertWheel(wheel)
        insertOptions(options)
    }

    @Transaction
    suspend fun deleteWheelWithOptions(wheelId: String) {
        getWheelById(wheelId)?.let { wheel ->
            deleteWheel(wheel)
            deleteOptionsByWheelId(wheelId)
            deleteHistoryByWheelId(wheelId)
        }
    }
}

