package com.wanbaohe.setting.skill.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.AppDatabase
import com.shifenmiao.database.ai.SkillFrontMatterParser
import com.shifenmiao.database.ai.entity.SkillEntity
import com.shifenmiao.storage.AIChatStorage
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AI 技能管理页组件。
 *
 * 数据接入照 SystemPromptManagementComponent 模式：直接注入 AppDatabase 拿 DAO，
 * feature/settings 不依赖 feature/ai；导入校验复用 core/database 的
 * [SkillFrontMatterParser]（与 AppDatabase 预置、feature/ai 导入同一规则）。
 */
class SkillManagementComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    private val appDatabase: AppDatabase,
    dispatchersHolder: DispatchersHolder,
) : BaseComponent(dispatchersHolder, componentContext) {

    companion object {
        private val SLUG_REGEX = Regex("[a-z0-9]+(-[a-z0-9]+)*")
    }

    private val skillDao = appDatabase.skillDao()

    /** 全局技能总开关 */
    val globalSkillsEnabled: StateFlow<Boolean> = AIChatStorage.isEnableSkills

    /** 全部技能（含停用），按 name 排序 */
    val skills: StateFlow<List<SkillEntity>> = skillDao.observeAll()
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 使用频次分档（阈值照 OpenMinis：20 / 60） */
    enum class UsageFrequency { NEVER, LOW, REGULAR, HIGH }

    fun usageFrequency(useCount: Double): UsageFrequency = when {
        useCount <= 0.0 -> UsageFrequency.NEVER
        useCount < 20.0 -> UsageFrequency.LOW
        useCount < 60.0 -> UsageFrequency.REGULAR
        else -> UsageFrequency.HIGH
    }

    fun setGlobalSkillsEnabled(enabled: Boolean) {
        AIChatStorage.saveIsEnableSkills(enabled)
    }

    fun setSkillEnabled(skill: SkillEntity, enabled: Boolean) {
        componentScope.launch {
            skillDao.update(
                skill.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /** 保存 LOCAL 技能（新建 / 编辑 body），强制 source=LOCAL。 */
    fun saveLocal(skill: SkillEntity) {
        componentScope.launch {
            skillDao.upsert(
                skill.copy(
                    source = SkillEntity.SOURCE_LOCAL,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 仅 LOCAL 技能可删除。 */
    fun deleteSkill(skill: SkillEntity) {
        if (skill.source != SkillEntity.SOURCE_LOCAL) return
        componentScope.launch {
            skillDao.deleteById(skill.id)
        }
    }

    /**
     * 从 SKILL.md 文本导入 LOCAL 技能。
     * frontmatter 只认单行 name/description，name 必须是 kebab-case slug，失败返回 false。
     */
    fun importFromContent(content: String, onResult: (Boolean) -> Unit = {}) {
        componentScope.launch {
            val body = content.trim()
            val meta = SkillFrontMatterParser.parse(body)
            if (meta == null || !SLUG_REGEX.matches(meta.first)) {
                onResult(false)
                return@launch
            }
            val (slug, description) = meta
            val now = System.currentTimeMillis()
            val existing = skillDao.getById(slug)
            skillDao.upsert(
                SkillEntity(
                    id = slug,
                    name = slug,
                    description = description,
                    body = body,
                    source = SkillEntity.SOURCE_LOCAL,
                    installedAt = existing?.installedAt ?: now,
                    updatedAt = now,
                )
            )
            onResult(true)
        }
    }

    /** BUNDLED 只读，改动走"另存为 LOCAL 副本"；id 冲突时追加序号。 */
    fun saveAsLocalCopy(skill: SkillEntity) {
        componentScope.launch {
            val now = System.currentTimeMillis()
            var copyId = "${skill.id}-copy"
            var sequence = 2
            while (skillDao.getById(copyId) != null) {
                copyId = "${skill.id}-copy$sequence"
                sequence++
            }
            skillDao.upsert(
                skill.copy(
                    id = copyId,
                    name = copyId,
                    source = SkillEntity.SOURCE_LOCAL,
                    documentId = null,
                    useCount = 0.0,
                    installedAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): SkillManagementComponent
    }
}
