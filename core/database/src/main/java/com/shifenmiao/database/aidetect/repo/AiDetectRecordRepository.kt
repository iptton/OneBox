package com.shifenmiao.database.aidetect.repo

import com.shifenmiao.database.aidetect.dao.AiDetectRecordDao
import com.shifenmiao.database.aidetect.entity.AiDetectRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI 检测历史仓库
 *
 * 只保留最近 [MAX_KEEP_COUNT] 条,插入后自动裁剪。删除类操作返回被移除的记录,
 * 由调用方负责清理关联的缩略图文件。
 */
class AiDetectRecordRepository(
    private val dao: AiDetectRecordDao,
) {

    fun observeAll(): Flow<List<AiDetectRecordEntity>> = dao.getAll()

    suspend fun getById(id: Long): AiDetectRecordEntity? = dao.getById(id)

    /** 插入记录并裁剪到上限,返回新记录 id 与被裁掉的记录 */
    suspend fun insert(record: AiDetectRecordEntity): Pair<Long, List<AiDetectRecordEntity>> {
        val newId = dao.insert(record)
        return newId to prune()
    }

    /** 删除指定记录,返回被删记录(不存在则为 null) */
    suspend fun delete(id: Long): AiDetectRecordEntity? {
        val removed = dao.getById(id) ?: return null
        dao.deleteById(id)
        return removed
    }

    /** 清空所有记录,返回被删记录 */
    suspend fun clearAll(): List<AiDetectRecordEntity> {
        val removedIds = dao.getIdsBeyond(0)
        if (removedIds.isEmpty()) return emptyList()
        val removed = removedIds.mapNotNull { dao.getById(it) }
        dao.deleteByIds(removedIds)
        return removed
    }

    /** 只保留最近 [MAX_KEEP_COUNT] 条,返回被裁掉的记录 */
    private suspend fun prune(): List<AiDetectRecordEntity> {
        val ids = dao.getIdsBeyond(MAX_KEEP_COUNT)
        if (ids.isEmpty()) return emptyList()
        val removed = ids.mapNotNull { dao.getById(it) }
        dao.deleteByIds(ids)
        return removed
    }

    companion object {
        const val MAX_KEEP_COUNT = 100
    }
}
