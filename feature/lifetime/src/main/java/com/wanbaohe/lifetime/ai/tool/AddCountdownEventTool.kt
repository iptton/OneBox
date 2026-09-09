package com.wanbaohe.lifetime.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.lifetime.R
import com.shifenmiao.lifetime.data.CountdownEventRepository
import com.shifenmiao.lifetime.domain.model.CountdownEvent
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import javax.inject.Inject
import org.json.JSONObject

/**
 * AI Agent 工具:`add_countdown_event` — 创建一条倒数日。
 *
 * target_date 只接受纯日期(yyyy-MM-dd)的公历日期;
 * 数据层的农历目标(每年滚动)与系统预置节日不通过本工具创建。
 * 非法输入返回 isError 并给出格式说明。
 */
class AddCountdownEventTool @Inject constructor(
    private val repository: CountdownEventRepository,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "add_countdown_event"

    override val description: String =
        textProvider.string(R.string.agent_tool_add_countdown_event_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_add_countdown_event_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_add_countdown_event_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SENSITIVE

    override val parallelizable: Boolean = false

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_add_countdown_event_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_add_countdown_event_examples)

    override val parametersSchema: ToolParameters = ToolParameters(
        properties = mapOf(
            "name" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_add_countdown_event_param_name),
            ),
            "target_date" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_add_countdown_event_param_target_date),
            ),
            "note" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_add_countdown_event_param_note),
            ),
        ),
        required = listOf("name", "target_date"),
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val json = if (arguments.isBlank()) JSONObject() else JSONObject(arguments)
            val eventName = json.optString("name").trim()
            require(eventName.isNotEmpty()) {
                textProvider.string(R.string.agent_tool_add_countdown_event_name_required)
            }

            val targetDateText = json.optString("target_date").trim()
            val targetDate = parseIsoDate(targetDateText)
                ?: error(textProvider.string(R.string.agent_tool_lifetime_invalid_date, targetDateText))

            val note = json.optString("note").takeIf { it.isNotBlank() }

            val eventId = repository.addCountdown(
                CountdownEvent(
                    name = eventName,
                    targetDate = targetDate,
                    note = note,
                )
            )

            val deeplink = lifetimeScreenDeeplink()
            lifetimeSuccessResult(
                markdown = buildString {
                    appendLine("# ${sanitizeMarkdownText(eventName)}")
                    appendLine()
                    appendLine("- ${textProvider.string(R.string.agent_tool_lifetime_label_target_date)}: $targetDate")
                    note?.let {
                        appendLine("- ${textProvider.string(R.string.agent_tool_lifetime_label_note)}: ${sanitizeMarkdownText(it)}")
                    }
                    appendLine()
                    appendLine("- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_lifetime_open_link), deeplink)}")
                }.trimEnd(),
                deepLinks = listOf(
                    toolDeepLinkJson(
                        uri = deeplink,
                        label = textProvider.string(R.string.agent_tool_lifetime_open_link),
                        primary = true,
                    )
                ),
            ) {
                put("eventId", eventId)
                put("targetDate", targetDate.toString())
            }
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_add_countdown_event_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_lifetime_unknown_error),
                ),
                isError = true,
            )
        }
    }
}
