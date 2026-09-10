package com.shifenmiao.database.period.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shifenmiao.database.period.entity.PeriodRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodRecordEntity)

    @Query("SELECT * FROM period_record WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PeriodRecordEntity?

    @Query("SELECT * FROM period_record WHERE record_date = :epochDay LIMIT 1")
    suspend fun getByDate(epochDay: Long): PeriodRecordEntity?

    @Query("SELECT * FROM period_record ORDER BY record_date DESC, created_at DESC")
    fun observeAll(): Flow<List<PeriodRecordEntity>>

    @Query("SELECT * FROM period_record ORDER BY record_date DESC, created_at DESC")
    suspend fun getAll(): List<PeriodRecordEntity>

    @Query("SELECT * FROM period_record WHERE record_date BETWEEN :startEpochDay AND :endEpochDay ORDER BY record_date DESC")
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<PeriodRecordEntity>>

    @Query("DELETE FROM period_record WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM period_record")
    suspend fun deleteAll()
}
