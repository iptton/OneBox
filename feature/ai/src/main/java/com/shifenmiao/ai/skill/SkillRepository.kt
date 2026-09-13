package com.shifenmiao.ai.skill

import androidx.room.withTransaction
import com.shifenmiao.ai.R
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.database.AppDatabase
import com.shifenmiao.database.ai.SkillFrontMatterParser
import com.shifenmiao.database.ai.dao.SkillDao
import com.shifenmiao.database.ai.entity.SkillEntity
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 技能（SKILL.md）仓库。
 *
 * - prompt 只注入 `<available_skills>` 清单（name/description），
 *   正文由 agent 经 use_skill 工具按需加载（[loadSkillBody]）；
 * - use_count 超 [NORMALIZE_THRESHOLD] 时全体归一化到 0–100（照 OpenMinis normalizeUseCounts）；
 * - BUNDLED 技能只读（版本递增时由 AppDatabase 预置 upsert 覆盖），
 *   用户改动走"另存为 LOCAL 副本"（[saveAsLocalCopy]）。
 */
@Singleton
class SkillRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val skillDao: SkillDao,
    private val textProvider: AgentToolTextProvider,
    dispatchersHolder: DispatchersHolder,
) {
    companion object {
        /** prompt 清单最多列出的技能数 */
        const val MAX_LISTED_SKILLS = 20

        /** use_count 超过该值时触发全体归一化 */
        const val NORMALIZE_THRESHOLD = 1000.0

        /** 清单遴选时"近期更新"的时间窗 */
        const val RECENT_UPDATE_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

        /** name/description 进 prompt 前的字符上限（防 `</available_skills>` 逃逸） */
        const val MAX_META_CHARS = 200

        private val SLUG_REGEX = Regex("[a-z0-9]+(-[a-z0-9]+)*")
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + dispatchersHolder.ioDispatcher)

    /** 全部技能（含停用），管理页列表与工具内查询共用。 */
    val skills: StateFlow<List<SkillEntity>> = skillDao.observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    /** 使用频次分档（阈值照 OpenMinis：20 / 60）。 */
    enum class UsageFrequency { NEVER, LOW, REGULAR, HIGH }

    fun usageFrequency(useCount: Double): UsageFrequency = when {
        useCount <= 0.0 -> UsageFrequency.NEVER
        useCount < 20.0 -> UsageFrequency.LOW
        useCount < 60.0 -> UsageFrequency.REGULAR
        else -> UsageFrequency.HIGH
    }

    // ─── use_skill 工具入口 ─────────────────────────────────────────────

    /**
     * 按 id（slug）或 name 取启用技能的完整正文，命中即记录使用频次。
     * 找不到 / 已停用返回 null，由工具层回传可用清单。
     */
    suspend fun loadSkillBody(nameOrSlug: String): String? {
        val key = nameOrSlug.trim()
        if (key.isEmpty()) return null
        val skill = skillDao.getById(key)
            ?: skillDao.getAll().firstOrNull { it.name.equals(key, ignoreCase = true) }
        if (skill == null || !skill.enabled) return null
        recordSkillUse(skill.id)
        return skill.body
    }

    /**
     * use_count 自增；任一技能超过 [NORMALIZE_THRESHOLD] 时全体归一化到 0–100（事务批量写回）。
     */
    suspend fun recordSkillUse(id: String) {
        val now = System.currentTimeMillis()
        skillDao.incrementUseCount(id, now)
        val all = skillDao.getAll()
        val max = all.maxOfOrNull { it.useCount } ?: return
        if (max <= NORMALIZE_THRESHOLD) return
        appDatabase.withTransaction {
            all.forEach { skill ->
                skillDao.updateUseCount(
                    id = skill.id,
                    useCount = skill.useCount / max * 100.0,
                    updatedAt = now
                )
            }
        }
    }

    // ─── prompt 注入 ────────────────────────────────────────────────────

    /**
     * 组装 `<available_skills>` 清单 fragment。
     *
     * enabled 技能 ≤ [MAX_LISTED_SKILLS] 直接全列；超出按
     * bundled > 7 天内更新 > useCount 遴选。
     * name/description 进 prompt 前做 XML 转义 + 单行化 + 截断。
     *
     * @return null 表示无启用技能
     */
    suspend fun buildPromptFragment(): String? {
        val enabled = skillDao.getAll().filter { it.enabled }
        if (enabled.isEmpty()) return null
        val selected = if (enabled.size <= MAX_LISTED_SKILLS) {
            enabled.sortedBy { it.name }
        } else {
            selectSkillsForListing(enabled)
        }
        return buildString {
            appendLine("<available_skills>")
            appendLine(textProvider.string(R.string.agent_skills_prompt_guidance))
            selected.forEach { skill ->
                appendLine("<skill>")
                appendLine("<name>${skill.name.toXmlSafe()}</name>")
                appendLine("<description>${skill.description.toXmlSafe()}</description>")
                appendLine("</skill>")
            }
            append("</available_skills>")
        }
    }

    /** 清单超长时的遴选：bundled 优先，其次 7 天内有更新，再按使用频次。 */
    private fun selectSkillsForListing(enabled: List<SkillEntity>): List<SkillEntity> {
        val now = System.currentTimeMillis()
        return enabled.sortedWith(
            compareByDescending<SkillEntity> { it.source == SkillEntity.SOURCE_BUNDLED }
                .thenByDescending { now - it.updatedAt <= RECENT_UPDATE_WINDOW_MS }
                .thenByDescending { it.useCount }
                .thenBy { it.name }
        ).take(MAX_LISTED_SKILLS)
    }

    private fun String.toXmlSafe(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace(Regex("\\s+"), " ")
            .take(MAX_META_CHARS)

    // ─── 管理 UI 用 CRUD ────────────────────────────────────────────────

    /** 保存 LOCAL 技能（新建 / 编辑），强制 source=LOCAL 并刷新 updated_at。 */
    suspend fun saveLocal(entity: SkillEntity) {
        skillDao.upsert(
            entity.copy(
                source = SkillEntity.SOURCE_LOCAL,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val skill = skillDao.getById(id) ?: return
        skillDao.update(
            skill.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        )
    }

    /** 仅 LOCAL 技能可删除；BUNDLED 由预置水位管理。 */
    suspend fun deleteLocal(id: String): Boolean {
        val skill = skillDao.getById(id) ?: return false
        if (skill.source != SkillEntity.SOURCE_LOCAL) return false
        skillDao.deleteById(id)
        return true
    }

    /**
     * 从 SKILL.md 文本导入 LOCAL 技能（剪贴板/文件导入入口）。
     * frontmatter 只认单行 name/description（与 AppDatabase 预置共用 [SkillFrontMatterParser]），
     * name 必须是 kebab-case slug，解析失败拒绝导入。
     */
    suspend fun importFromContent(content: String): Result<SkillEntity> {
        val body = content.trim()
        val invalid by lazy {
            Result.failure<SkillEntity>(
                IllegalArgumentException(textProvider.string(R.string.agent_skill_import_invalid))
            )
        }
        val (slug, description) = SkillFrontMatterParser.parse(body) ?: return invalid
        if (!SLUG_REGEX.matches(slug)) return invalid
        val now = System.currentTimeMillis()
        val existing = skillDao.getById(slug)
        val entity = SkillEntity(
            id = slug,
            name = slug,
            description = description,
            body = body,
            source = SkillEntity.SOURCE_LOCAL,
            installedAt = existing?.installedAt ?: now,
            updatedAt = now,
        )
        skillDao.upsert(entity)
        return Result.success(entity)
    }

    /** BUNDLED 只读，用户改动走"另存为 LOCAL 副本"；id 冲突时追加序号。 */
    suspend fun saveAsLocalCopy(id: String): SkillEntity? {
        val source = skillDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        var copyId = "${source.id}-copy"
        var sequence = 2
        while (skillDao.getById(copyId) != null) {
            copyId = "${source.id}-copy$sequence"
            sequence++
        }
        val copy = source.copy(
            id = copyId,
            name = copyId,
            source = SkillEntity.SOURCE_LOCAL,
            documentId = null,
            useCount = 0.0,
            installedAt = now,
            updatedAt = now,
        )
        skillDao.upsert(copy)
        return copy
    }
}
