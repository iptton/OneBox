package com.shifenmiao.database.recordcenter.repo

import com.shifenmiao.database.FeatureDatabase
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRecordRepository @Inject constructor(
    private val database: FeatureDatabase,
) {

    suspend fun upsert(record: HealthRecordEntity) {
        database.healthRecordDao().upsert(record)
    }

    suspend fun deleteById(id: String) {
        database.healthRecordDao().deleteById(id)
    }

    fun observeByType(type: String): Flow<List<HealthRecordEntity>> {
        return database.healthRecordDao().observeByType(type)
    }

    fun observeRange(type: String, from: Long, to: Long): Flow<List<HealthRecordEntity>> {
        return database.healthRecordDao().observeRange(type, from, to)
    }

    fun observeLatestPerType(): Flow<List<HealthRecordEntity>> {
        return database.healthRecordDao().observeLatestPerType()
    }

    suspend fun getById(id: String): HealthRecordEntity? {
        return database.healthRecordDao().getById(id)
    }

    suspend fun getRangeOnce(type: String, from: Long, to: Long): List<HealthRecordEntity> {
        return database.healthRecordDao().getRangeOnce(type, from, to)
    }

    suspend fun getLatestPerTypeOnce(): List<HealthRecordEntity> {
        return database.healthRecordDao().getLatestPerTypeOnce()
    }
}
