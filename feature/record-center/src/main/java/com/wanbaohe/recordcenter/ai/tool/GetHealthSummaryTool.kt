package com.wanbaohe.recordcenter.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.common.handle.navigation.AppNavigationRegistry
import com.shifenmiao.common.handle.navigation.AppNavigationTargetType
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.model.RecordFieldsCodec
import com.wanbaohe.recordcenter.registry.RecordTypeCatalog
import com.wanbaohe.recordcenter.service.RecordCenterService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * AI Agent 工具:`get_health_summary` — 各记录类型最新一条数据的总览(含单位与参考范围)。
 */
class GetHealthSummaryTool @Inject constructor(
    private val service: RecordCenterService,
    private val catalog: RecordTypeCatalog,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "get_health_summary"

    override val description: String =
        textProvider.string(R.string.agent_tool_get_health_summary_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_get_health_summary_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_get_health_summary_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SAFE

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_get_health_summary_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_get_health_summary_examples)

    override val parametersSchema: ToolParameters = ToolParameters()

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val latestByType = service.latestPerType().associateBy { it.type }

            val deeplink = AppNavigationRegistry.buildStructuredDeeplink(
                targetType = AppNavigationTargetType.SCREEN,
                routeKey = AddHealthRecordTool.RECORD_CENTER_ROUTE_KEY,
                params = mapOf("type" to "list"),
            )

            if (latestByType.isEmpty()) {
                return AgentToolResult(
                    content = buildString {
                        appendLine("# ${sanitizeMarkdownText(title)}")
                        appendLine()
                        appendLine(textProvider.string(R.string.agent_tool_get_health_summary_empty))
                        appendLine()
                        appendLine(
                            "- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_record_center_open_link), deeplink)}"
                        )
                    }.trimEnd(),
                )
            }

            AgentToolResult(
                content = buildString {
                    appendLine("# ${sanitizeMarkdownText(title)}")
                    appendLine()
                    catalog.all().forEach { definition ->
                        val entity = latestByType[definition.key] ?: return@forEach
                        val fields = RecordFieldsCodec.decode(entity.fieldsJson)
                        val timeText = Instant.ofEpochMilli(entity.happenedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DATE_FORMATTER)
                        appendLine("## ${sanitizeMarkdownText(textProvider.string(definition.titleRes))}")
                        definition.fields.forEach { field ->
                            val value = fields[field.key] ?: return@forEach
                            val unit = field.unit.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                            appendLine(
                                "- ${sanitizeMarkdownText(textProvider.string(field.labelRes))}: " +
                                    "${RecordFieldsCodec.formatValue(value)}$unit"
                            )
                        }
                        appendLine("- ${textProvider.string(R.string.record_center_label_time)}: $timeText")
                        definition.referenceRangeRes?.let {
                            appendLine("- ${textProvider.string(it)}")
                        }
                        appendLine()
                    }
                    appendLine(
                        "- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_record_center_open_link), deeplink)}"
                    )
                }.trimEnd(),
            )
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_get_health_summary_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_record_center_unknown_error),
                ),
                isError = true,
            )
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
