package com.wanbaohe.recordcenter.service

import com.shifenmiao.common.ai.AIPromptExecutor
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.shifenmiao.interfaces.singleton.AppContext
import com.wanbaohe.recordcenter.data.HealthProfile
import com.wanbaohe.recordcenter.model.RecordFieldsCodec
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康记录 AI 解读服务(对齐 MilestoneInsightService)。
 *
 * 编排逻辑:
 * 1. 按记录类型定义 + 最近若干条记录拼装输入,调用 [AIPromptExecutor] 生成解读
 * 2. 失败/引擎未配置时只返回失败原因
 *
 * 与里程碑/诗词不同:数据变化频繁,解读结果**不落库、不缓存**,由页面手动触发生成。
 */
@Singleton
class HealthInsightService @Inject constructor(
    private val aiExecutor: AIPromptExecutor,
) {

    /**
     * 解读当前筛选范围内的记录。
     *
     * @param definition 记录类型定义(标题/字段/参考范围经 string resources 本地化)
     * @param records    当前范围内的记录(任意顺序,内部按时间倒序截取最近 [MAX_RECORDS] 条)
     * @param rangeLabel 已本地化的时间范围文案,如「近 30 天」
     * @param profile    用户基础信息(性别/年龄/身高/体重),已设置时作为上下文带上
     */
    suspend fun interpret(
        definition: RecordTypeDefinition,
        records: List<HealthRecordEntity>,
        rangeLabel: String,
        profile: HealthProfile? = null,
    ): GenerationResult {
        val input = buildInput(definition, records, rangeLabel, profile)
        val result = aiExecutor.execute(
            systemPrompt = SYSTEM_PROMPT,
            input = input,
            // 扣费由 RecordListComponent 在成功后按其固定额度处理,避免重复扣
            billing = AIPromptExecutor.PromptBilling.EXTERNAL,
        )
        if (!result.isSuccess) {
            return GenerationResult.Failed(result.errorMessage.orEmpty())
        }
        val content = result.content.trim()
        if (content.isBlank()) {
            return GenerationResult.Failed("AI 返回内容为空")
        }
        return GenerationResult.Success(content)
    }

    private fun buildInput(
        definition: RecordTypeDefinition,
        records: List<HealthRecordEntity>,
        rangeLabel: String,
        profile: HealthProfile?,
    ): String = buildString {
        profile?.takeIf { it.isSet }?.let { p ->
            val parts = mutableListOf<String>()
            when (p.gender) {
                HealthProfile.Gender.MALE -> parts += "男性"
                HealthProfile.Gender.FEMALE -> parts += "女性"
                HealthProfile.Gender.UNSET -> {}
            }
            p.age?.let { parts += "${it}岁" }
            p.heightCm?.let { parts += "身高${RecordFieldsCodec.formatValue(it)}cm" }
            p.weightKg?.let { parts += "体重${RecordFieldsCodec.formatValue(it)}kg" }
            if (parts.isNotEmpty()) {
                append("用户基本信息:").append(parts.joinToString(",")).append('\n')
            }
        }
        append("记录类型:").append(AppContext.getString(definition.titleRes)).append('\n')
        definition.referenceRangeRes?.let {
            append("参考范围:").append(AppContext.getString(it)).append('\n')
        }
        append("字段:").append(
            definition.fields.joinToString("、") { field ->
                if (field.unit.isEmpty()) AppContext.getString(field.labelRes)
                else "${AppContext.getString(field.labelRes)}(${field.unit})"
            }
        ).append('\n')
        append("时间范围:").append(rangeLabel).append('\n')
        append("记录(最新在前):\n")
        records.sortedByDescending { it.happenedAt }
            .take(MAX_RECORDS)
            .forEach { entity ->
                val values = RecordFieldsCodec.decode(entity.fieldsJson)
                val valueText = definition.fields.mapNotNull { field ->
                    values[field.key]?.let { value ->
                        val formatted = RecordFieldsCodec.formatValue(value)
                        val label = AppContext.getString(field.labelRes)
                        if (field.unit.isEmpty()) "$label $formatted"
                        else "$label $formatted ${field.unit}"
                    }
                }.joinToString(",")
                val timeText = Instant.ofEpochMilli(entity.happenedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER)
                append(timeText).append(' ').append(valueText).append('\n')
            }
    }

    sealed interface GenerationResult {
        data class Success(val content: String) : GenerationResult
        data class Failed(val reason: String) : GenerationResult
    }

    companion object {
        /** 送给 AI 的记录条数上限,避免输入过长 */
        private const val MAX_RECORDS = 30

        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")

        private const val SYSTEM_PROMPT =
            "你是一位谨慎的健康顾问。请根据用户提供的健康记录数据,用不超过150字解读数据趋势," +
                "指出可能的异常,并给出日常生活建议。语言口语化,分2-3段。" +
                "最后必须声明:以上仅供参考,不构成医疗诊断。直接输出正文,不要加标题或多余说明。"
    }
}
