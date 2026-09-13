package com.wanbaohe.setting.skill.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.AppDatabase
import com.shifenmiao.database.ai.SkillFrontMatterParser
import com.shifenmiao.database.ai.SkillImportValidator
import com.shifenmiao.database.ai.SkillUsagePolicy
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
 * feature/settings 不依赖 feature/ai；导入/保存校验与使用频次分档统一走
 * core/database 的 [SkillImportValidator] / [SkillUsagePolicy] /
 * [SkillFrontMatterParser]（与 AppDatabase 预置、feature/ai 同一规则）。
 */
class SkillManagementComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    private val appDatabase: AppDatabase,
    dispatchersHolder: DispatchersHolder,
) : BaseComponent(dispatchersHolder, componentContext) {

    private val skillDao = appDatabase.skillDao()

    /** 全局技能总开关 */
    val globalSkillsEnabled: StateFlow<Boolean> = AIChatStorage.isEnableSkills

    /** 全部技能（含停用），按 name 排序 */
    val skills: StateFlow<List<SkillEntity>> = skillDao.observeAll()
        .stateIn(componentScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun usageFrequency(useCount: Double): SkillUsagePolicy.UsageFrequency =
        SkillUsagePolicy.frequencyOf(useCount)

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

    /**
     * 保存 LOCAL 技能（新建 / 编辑 body）。
     * 保存前重新解析 frontmatter：失败（含空正文）拒绝；成功同步 name/description（id 不动）。
     */
    fun saveLocal(skill: SkillEntity, onResult: (Boolean) -> Unit = {}) {
        componentScope.launch {
            val validated = SkillImportValidator.validate(skill.body).getOrNull()
            if (validated == null) {
                onResult(false)
                return@launch
            }
            skillDao.upsert(
                skill.copy(
                    name = validated.slug,
                    description = validated.description,
                    body = validated.body,
                    source = SkillEntity.SOURCE_LOCAL,
                    updatedAt = System.currentTimeMillis()
                )
            )
            onResult(true)
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
     * 从 SKILL.md 文本导入 LOCAL 技能（剪贴板 / 文件导入共用管线）。
     *
     * - 已存在同名 BUNDLED → 拒绝（BUNDLED_NAME_CONFLICT）；
     * - 已存在 LOCAL → 更新语义：保留 enabled / use_count / installed_at，
     *   只更新 name/description/body/updated_at。
     *
     * @param onResult null = 成功；否则为拒绝原因，由页面映射文案
     */
    fun importFromContent(
        content: String,
        onResult: (SkillImportValidator.Rejection?) -> Unit = {}
    ) {
        componentScope.launch {
            val body = content.trim()
            val meta = SkillFrontMatterParser.parse(body)
            val existing = meta?.let { skillDao.getById(it.first) }
            SkillImportValidator.validate(body, existing).fold(
                onSuccess = { validated ->
                    val now = System.currentTimeMillis()
                    if (existing != null) {
                        skillDao.update(
                            existing.copy(
                                name = validated.slug,
                                description = validated.description,
                                body = validated.body,
                                updatedAt = now,
                            )
                        )
                    } else {
                        skillDao.upsert(
                            SkillEntity(
                                id = validated.slug,
                                name = validated.slug,
                                description = validated.description,
                                body = validated.body,
                                source = SkillEntity.SOURCE_LOCAL,
                                installedAt = now,
                                updatedAt = now,
                            )
                        )
                    }
                    onResult(null)
                },
                onFailure = { error ->
                    onResult(
                        (error as? SkillImportValidator.SkillImportException)?.rejection
                            ?: SkillImportValidator.Rejection.INVALID_FRONTMATTER
                    )
                }
            )
        }
    }

    /**
     * BUNDLED 只读，改动走"另存为 LOCAL 副本"；id 冲突时追加序号。
     * 副本 body 的 frontmatter name 同步改写为新名字，保证"导出副本再导入"命中副本。
     */
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
                    body = SkillFrontMatterParser.rewriteName(skill.body, copyId),
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
