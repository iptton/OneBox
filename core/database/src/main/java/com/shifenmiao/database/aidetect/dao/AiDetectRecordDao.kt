package com.shifenmiao.database.aidetect.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shifenmiao.database.aidetect.entity.AiDetectRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDetectRecordDao {

    /** 插入一条检测记录,返回自增 id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AiDetectRecordEntity): Long

    /** 按时间倒序获取所有记录(Flow,实时更新) */
    @Query("SELECT * FROM ai_detect_record ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AiDetectRecordEntity>>

    /** 删除指定记录 */
    @Query("DELETE FROM ai_detect_record WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 清空所有记录 */
    @Query("DELETE FROM ai_detect_record")
    suspend fun clearAll()

    /** 超出保留上限的最旧记录 id 列表(用于裁剪及清理缩略图文件) */
    @Query(
        "SELECT id FROM ai_detect_record ORDER BY createdAt DESC LIMIT -1 OFFSET :keepCount"
    )
    suspend fun getIdsBeyond(keepCount: Int): List<Long>

    /** 按 id 列表删除记录 */
    @Query("DELETE FROM ai_detect_record WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** 按 id 查询单条记录 */
    @Query("SELECT * FROM ai_detect_record WHERE id = :id")
    suspend fun getById(id: Long): AiDetectRecordEntity?
}
