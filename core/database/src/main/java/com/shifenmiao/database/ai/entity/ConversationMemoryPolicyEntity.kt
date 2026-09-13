package com.shifenmiao.database.ai.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 会话级记忆/技能开关：
 * 主键用 conversation.id（真会话身份），不用工具策略的 scope key——
 * 后者对无 entryRefId 的普通聊天一律退化为 CHAT:default。
 */
@Entity(tableName = "conversation_memory_policy")
data class ConversationMemoryPolicyEntity(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    /** 本会话是否注入记忆 + 是否发 memory 工具。 */
    @ColumnInfo(name = "memory_enabled", defaultValue = "1")
    val memoryEnabled: Boolean = true,
    /** 本会话是否注入技能清单 + 是否发 use_skill。 */
    @ColumnInfo(name = "skills_enabled", defaultValue = "1")
    val skillsEnabled: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
