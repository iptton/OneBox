package com.wanbaohe.recordcenter.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.common.handle.navigation.AppNavigationRegistry
import com.shifenmiao.common.handle.navigation.AppNavigationTargetType
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.model.RecordFieldsCodec
import com.wanbaohe.recordcenter.registry.RecordTypeCatalog
import com.wanbaohe.recordcenter.service.RecordCenterService
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * AI Agent 工具:`add_health_record` — 添加一条健康记录。
 *
 * record_type 的枚举与描述由 RecordTypeCatalog 动态生成;
 * values 是 字段名→数值 的自由 JSON object(schema 无法表达动态字段,
 * 字段契约写在工具描述与 values 参数描述里,由 Service 做最终校验)。
 */
class AddHealthRecordTool @Inject constructor(
    private val service: RecordCenterService,
    catalog: RecordTypeCatalog,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    private val schemaSupport = RecordCenterToolSchemaSupport(catalog, textProvider)

    override val name: String = "add_health_record"

    override val description: String =
        textProvider.string(R.string.agent_tool_add_health_record_description) +
            "\n" + schemaSupport.buildTypeFieldsDescription()

    override val title: String =
        textProvider.string(R.string.agent_tool_add_health_record_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_add_health_record_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SENSITIVE

    override val parallelizable: Boolean = false

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_add_health_record_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_add_health_record_examples)

    override val parametersSchema: ToolParameters = ToolParameters(
        type = "object",
        properties = mapOf(
            "record_type" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_add_health_record_param_record_type),
                enum = schemaSupport.recordTypeEnum(),
            ),
            "values" to ToolParameterProperty(
                type = "object",
                description = textProvider.string(R.string.agent_tool_add_health_record_param_values),
            ),
            "happened_at" to ToolParameterProperty(
                type = "integer",
                description = textProvider.string(R.string.agent_tool_add_health_record_param_happened_at),
            ),
            "note" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_add_health_record_param_note),
            ),
        ),
        required = listOf("record_type", "values"),
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val json = if (arguments.isBlank()) JSONObject() else JSONObject(arguments)

            val typeKey = json.optString("record_type").takeIf { it.isNotBlank() }
                ?: error(textProvider.string(R.string.record_center_error_unknown_type, ""))
            val definition = schemaSupport.requireType(typeKey)

            val valuesJson = json.optJSONObject("values")
                ?: error(
                    textProvider.string(
                        R.string.record_center_error_missing_field,
                        textProvider.string(definition.fields.first().labelRes),
                    )
                )
            val values = buildMap {
                valuesJson.keys().forEach { key ->
                    put(key, valuesJson.getDouble(key).toFloat())
                }
            }

            val happenedAt = json.optLong("happened_at").takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val note = json.optString("note").takeIf { it.isNotBlank() }

            val entity = service.addRecord(
                typeKey = typeKey,
                values = values,
                happenedAt = happenedAt,
                note = note,
                actor = RecordCenterService.ACTOR_AGENT,
            ).getOrThrow()

            val deeplink = AppNavigationRegistry.buildStructuredDeeplink(
                targetType = AppNavigationTargetType.SCREEN,
                routeKey = RECORD_CENTER_ROUTE_KEY,
                params = mapOf("type" to "list", "record_type" to typeKey),
            )
            val timeText = Instant.ofEpochMilli(happenedAt)
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER)

            AgentToolResult(
                content = buildString {
                    appendLine("# ${sanitizeMarkdownText(title)}")
                    appendLine()
                    appendLine(
                        "- **${sanitizeMarkdownText(textProvider.string(definition.titleRes))}** · " +
                            textProvider.string(R.string.agent_tool_add_health_record_success)
                    )
                    definition.fields.forEach { field ->
                        val value = RecordFieldsCodec.decode(entity.fieldsJson)[field.key]
                            ?: return@forEach
                        val unit = field.unit.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                        appendLine(
                            "- ${sanitizeMarkdownText(textProvider.string(field.labelRes))}: " +
                                "${RecordFieldsCodec.formatValue(value)}$unit"
                        )
                    }
                    appendLine("- **${textProvider.string(R.string.record_center_label_time)}**: $timeText")
                    note?.let {
                        appendLine(
                            "- **${textProvider.string(R.string.record_center_label_note)}**: ${sanitizeMarkdownText(it)}"
                        )
                    }
                    appendLine()
                    appendLine(
                        "- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_record_center_open_link), deeplink)}"
                    )
                }.trimEnd(),
            )
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_add_health_record_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_record_center_unknown_error),
                ),
                isError = true,
            )
        }
    }

    companion object {
        /** 记录中心路由 key,Screen 注册在 UI 任务中落地,这里先对齐 deeplink 契约 */
        internal const val RECORD_CENTER_ROUTE_KEY = "record_center"

        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
