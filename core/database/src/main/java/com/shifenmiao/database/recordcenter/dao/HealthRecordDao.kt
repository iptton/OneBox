package com.shifenmiao.database.recordcenter.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: HealthRecordEntity)

    @Query("DELETE FROM health_record WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM health_record WHERE type = :type ORDER BY happened_at DESC")
    fun observeByType(type: String): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_record WHERE type = :type AND happened_at BETWEEN :from AND :to ORDER BY happened_at ASC")
    fun observeRange(type: String, from: Long, to: Long): Flow<List<HealthRecordEntity>>

    // 每个 type 的最新一条记录（按 happened_at 最大值；同 type 同毫秒时间戳的极端并列会同时返回）
    @Query(
        """
        SELECT r.* FROM health_record r
        INNER JOIN (
            SELECT type, MAX(happened_at) AS max_happened_at
            FROM health_record
            GROUP BY type
        ) latest ON latest.type = r.type AND latest.max_happened_at = r.happened_at
        ORDER BY r.type ASC
        """
    )
    fun observeLatestPerType(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_record WHERE id = :id")
    suspend fun getById(id: String): HealthRecordEntity?

    @Query("SELECT * FROM health_record WHERE type = :type AND happened_at BETWEEN :from AND :to ORDER BY happened_at ASC")
    suspend fun getRangeOnce(type: String, from: Long, to: Long): List<HealthRecordEntity>

    @Query(
        """
        SELECT r.* FROM health_record r
        INNER JOIN (
            SELECT type, MAX(happened_at) AS max_happened_at
            FROM health_record
            GROUP BY type
        ) latest ON latest.type = r.type AND latest.max_happened_at = r.happened_at
        ORDER BY r.type ASC
        """
    )
    suspend fun getLatestPerTypeOnce(): List<HealthRecordEntity>
}
