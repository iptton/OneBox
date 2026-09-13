package com.shifenmiao.database.ai.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shifenmiao.database.ai.entity.ConversationMemoryPolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMemoryPolicyDao {

    /**
     * 保存会话级记忆/技能开关。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationMemoryPolicyEntity)

    @Query("SELECT * FROM conversation_memory_policy WHERE conversation_id = :conversationId LIMIT 1")
    suspend fun getByConversationId(conversationId: String): ConversationMemoryPolicyEntity?

    @Query("SELECT * FROM conversation_memory_policy WHERE conversation_id = :conversationId LIMIT 1")
    fun observeByConversationId(conversationId: String): Flow<ConversationMemoryPolicyEntity?>

    @Query("DELETE FROM conversation_memory_policy WHERE conversation_id = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)
}
