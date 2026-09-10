package com.shifenmiao.database.period.repo

import com.shifenmiao.database.period.dao.PeriodRecordDao
import com.shifenmiao.database.period.entity.PeriodRecordEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PeriodRecordRepository @Inject constructor(
    private val dao: PeriodRecordDao,
) {

    fun observeAll(): Flow<List<PeriodRecordEntity>> = dao.observeAll()

    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<PeriodRecordEntity>> =
        dao.observeInRange(startEpochDay, endEpochDay)

    suspend fun getById(id: String): PeriodRecordEntity? = dao.getById(id)

    suspend fun getByDate(epochDay: Long): PeriodRecordEntity? = dao.getByDate(epochDay)

    suspend fun getAll(): List<PeriodRecordEntity> = dao.getAll()

    suspend fun upsert(entity: PeriodRecordEntity) {
        dao.upsert(entity)
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}
