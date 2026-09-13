package com.shifenmiao.ai.memory

import com.shifenmiao.ai.R
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.ai.context.TokenEstimator
import com.shifenmiao.database.ai.dao.MemoryEntryDao
import com.shifenmiao.database.ai.entity.MemoryEntryEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 记忆仓库：两级记忆（profile 全局档案 + log 时间序日志）的写入、检索与注入组装。
 *
 * 保留 OpenMinis 的语义（关键词检索、注入上限、滚动淘汰），载体落到 Room：
 * - 检索不用 SQL LIKE（要转义 %/_，且 SQLite LIKE 仅 ASCII 大小写不敏感），
 *   DAO 取出 enabled 候选后在 Kotlin 侧 lowercase() 过滤（Unicode 感知）；
 * - log 滚动淘汰（超 [MAX_LOG_ENTRIES] 删最旧），profile 不参与；
 * - 注入 = 全部 enabled profile + 最近 3 个"有内容的日期桶"的 log，再按预算截尾。
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val memoryEntryDao: MemoryEntryDao,
    private val textProvider: AgentToolTextProvider,
) {
    companion object {
        /** log 条目滚动淘汰上限（profile 不参与） */
        const val MAX_LOG_ENTRIES = 500

        /** 关键词搜索返回上限 */
        const val SEARCH_RESULT_LIMIT = 60

        /** 无关键词 dump 返回上限 */
        const val DUMP_RESULT_LIMIT = 500

        /** 搜索/ dump 总输出字节上限（UTF-8） */
        const val MAX_OUTPUT_BYTES = 30 * 1024

        /** 注入的 log 日期桶数量（最近 N 个有内容的日期，不是近 N 天） */
        const val PROMPT_LOG_BUCKETS = 3

        /** 记忆 fragment 占 prompt 预算的比例上限 */
        const val PROMPT_BUDGET_FRACTION = 0.15
    }

    /** 搜索范围：仅 log / 全部（profile + log） */
    enum class SearchScope { LOG, ALL }

    data class MemorySearchResult(
        val entries: List<MemoryEntryEntity>,
        /** 命中数超过条数上限被截断 */
        val truncatedByCount: Boolean,
        /** 输出超过字节上限被截断 */
        val truncatedByBytes: Boolean,
    )

    /**
     * 写入一条 log 记忆（memory_write 工具入口），写后执行滚动淘汰。
     *
     * @return 新条目 id
     */
    suspend fun writeMemory(content: String, sourceConversationId: String?): Long {
        val now = System.currentTimeMillis()
        val id = memoryEntryDao.insert(
            MemoryEntryEntity(
                kind = MemoryEntryEntity.KIND_LOG,
                content = content.trim(),
                sourceConversationId = sourceConversationId?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            )
        )
        // 滚动淘汰：log 超上限删最旧，profile 不参与
        val total = memoryEntryDao.countByKind(MemoryEntryEntity.KIND_LOG)
        if (total > MAX_LOG_ENTRIES) {
            memoryEntryDao.deleteOldestByKind(MemoryEntryEntity.KIND_LOG, total - MAX_LOG_ENTRIES)
        }
        return id
    }

    /**
     * 条目级关键词搜索：全部关键词命中即返回整条，按时间倒序。
     * 无关键词时为 dump（取最新条目）。上限：搜索 [SEARCH_RESULT_LIMIT] 条 /
     * dump [DUMP_RESULT_LIMIT] 条 / 总输出 [MAX_OUTPUT_BYTES] 字节。
     */
    suspend fun search(keywords: List<String>, scope: SearchScope): MemorySearchResult {
        val normalizedKeywords = keywords
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
        val candidates = buildList {
            if (scope == SearchScope.ALL) {
                addAll(memoryEntryDao.getByKind(MemoryEntryEntity.KIND_PROFILE, enabled = true))
            }
            addAll(
                memoryEntryDao.getRecentByKind(
                    kind = MemoryEntryEntity.KIND_LOG,
                    enabled = true,
                    limit = DUMP_RESULT_LIMIT
                )
            )
        }
        val limit = if (normalizedKeywords.isEmpty()) DUMP_RESULT_LIMIT else SEARCH_RESULT_LIMIT
        val matched = candidates
            .filter { entry ->
                normalizedKeywords.all { keyword -> keyword in entry.content.lowercase() }
            }
            .sortedByDescending { it.createdAt }

        val entries = mutableListOf<MemoryEntryEntity>()
        var usedBytes = 0
        var truncatedByBytes = false
        for (entry in matched.take(limit)) {
            val entryBytes = entry.content.toByteArray(Charsets.UTF_8).size
            if (usedBytes + entryBytes > MAX_OUTPUT_BYTES) {
                truncatedByBytes = true
                break
            }
            usedBytes += entryBytes
            entries.add(entry)
        }
        return MemorySearchResult(
            entries = entries,
            truncatedByCount = matched.size > limit,
            truncatedByBytes = truncatedByBytes
        )
    }

    /**
     * 组装注入 system prompt 的记忆 fragment（`<memory>` 标签包裹 + 防御性引导语）。
     *
     * 内容：全部 enabled profile + 最近 [PROMPT_LOG_BUCKETS] 个有内容日期桶的 log。
     * 整体按 [tokenBudget] 的 [PROMPT_BUDGET_FRACTION] 用 TokenEstimator 截尾：
     * 超预算优先丢最老日期桶，profile 与引导语恒定保留。
     *
     * @return null 表示无任何可注入内容
     */
    suspend fun buildPromptFragment(tokenBudget: Int): String? {
        val profiles = memoryEntryDao.getByKind(MemoryEntryEntity.KIND_PROFILE, enabled = true)
        val logs = memoryEntryDao.getRecentByKind(
            kind = MemoryEntryEntity.KIND_LOG,
            enabled = true,
            limit = MAX_LOG_ENTRIES
        )
        if (profiles.isEmpty() && logs.isEmpty()) return null

        // logs 已按 created_at 倒序，groupBy 保持首次出现顺序 = 日期桶倒序
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val buckets = logs.groupBy { dateFormat.format(Date(it.createdAt)) }
        val budgetTokens = (tokenBudget * PROMPT_BUDGET_FRACTION).toInt()

        var selectedDates = buckets.keys.take(PROMPT_LOG_BUCKETS)
        while (selectedDates.isNotEmpty()) {
            val text = renderFragment(profiles, buckets, selectedDates)
            if (budgetTokens <= 0 || TokenEstimator.estimateText(text) <= budgetTokens) {
                return text
            }
            // 超预算优先丢最老日期桶
            selectedDates = selectedDates.dropLast(1)
        }
        return renderFragment(profiles, buckets, emptyList())
    }

    private fun renderFragment(
        profiles: List<MemoryEntryEntity>,
        buckets: Map<String, List<MemoryEntryEntity>>,
        selectedDates: List<String>
    ): String {
        return buildString {
            appendLine("<memory>")
            appendLine(textProvider.string(R.string.agent_memory_prompt_guidance))
            if (profiles.isNotEmpty()) {
                appendLine()
                appendLine(textProvider.string(R.string.agent_memory_prompt_profile_header))
                profiles.forEach { entry ->
                    appendLine("- ${entry.content.trim()}")
                }
            }
            selectedDates.forEach { date ->
                val entries = buckets[date].orEmpty()
                if (entries.isEmpty()) return@forEach
                appendLine()
                appendLine(textProvider.string(R.string.agent_memory_prompt_log_header, date))
                entries.forEach { entry ->
                    appendLine("- ${entry.content.trim()}")
                }
            }
            append("</memory>")
        }
    }

    // ─── 管理 UI 用 CRUD ────────────────────────────────────────────────

    fun observeByKind(kind: String, enabled: Boolean): Flow<List<MemoryEntryEntity>> {
        return memoryEntryDao.observeByKind(kind, enabled)
    }

    /** 新增（id=0）或更新（id>0）条目，更新时刷新 updated_at。 */
    suspend fun saveEntry(entry: MemoryEntryEntity): Long {
        val stamped = entry.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) {
            memoryEntryDao.insert(stamped)
        } else {
            memoryEntryDao.update(stamped)
            stamped.id
        }
    }

    suspend fun deleteById(id: Long) {
        memoryEntryDao.deleteById(id)
    }

    /** 一键清空 log（profile 不动）。 */
    suspend fun clearLog() {
        memoryEntryDao.clearByKind(MemoryEntryEntity.KIND_LOG)
    }
}
