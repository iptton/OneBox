package com.shifenmiao.ai.memory

import com.shifenmiao.database.ai.dao.ConversationMemoryPolicyDao
import com.shifenmiao.database.ai.entity.ConversationMemoryPolicyEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话级记忆/技能开关仓库。
 *
 * 主键用 conversation.id（真会话身份），不用工具策略的 scope key——
 * 后者对无 entryRefId 的普通聊天一律退化为 CHAT:default。
 * 无策略行 = 默认全开（true）。
 */
@Singleton
class ConversationMemoryPolicyRepository @Inject constructor(
    private val dao: ConversationMemoryPolicyDao
) {

    suspend fun getPolicy(conversationId: String): ConversationMemoryPolicyEntity? {
        if (conversationId.isBlank()) return null
        return dao.getByConversationId(conversationId)
    }

    fun observePolicy(conversationId: String): Flow<ConversationMemoryPolicyEntity?> {
        return dao.observeByConversationId(conversationId)
    }

    suspend fun setMemoryEnabled(conversationId: String, enabled: Boolean) {
        if (conversationId.isBlank()) return
        // 先初始化行（不存在则插入默认值行），再字段级更新，消除 read-modify-write 竞态
        dao.insertIgnore(ConversationMemoryPolicyEntity(conversationId = conversationId))
        dao.setMemoryEnabled(conversationId, enabled, System.currentTimeMillis())
    }

    suspend fun setSkillsEnabled(conversationId: String, enabled: Boolean) {
        if (conversationId.isBlank()) return
        dao.insertIgnore(ConversationMemoryPolicyEntity(conversationId = conversationId))
        dao.setSkillsEnabled(conversationId, enabled, System.currentTimeMillis())
    }
}
