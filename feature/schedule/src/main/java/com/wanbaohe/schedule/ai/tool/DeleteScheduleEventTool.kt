package com.wanbaohe.schedule.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import com.wanbaohe.schedule.R
import com.wanbaohe.schedule.service.ScheduleService
import javax.inject.Inject
import org.json.JSONObject

/**
 * AI Agent 工具:`delete_schedule_event` — 按事件 id 删除一条本地日程事件。
 *
 * 删除会连带清除系统日历等 provider 绑定,属于不可逆操作,
 * 因此 requiresConfirmation=true,执行前必须由用户确认。
 * event_id 通常来自 `query_schedule_events` 的查询结果。
 */
class DeleteScheduleEventTool @Inject constructor(
    private val service: ScheduleService,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "delete_schedule_event"

    override val description: String =
        textProvider.string(R.string.agent_tool_delete_schedule_event_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_delete_schedule_event_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_delete_schedule_event_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.DANGEROUS

    override val requiresConfirmation: Boolean = true

    override val confirmationTitle: String =
        textProvider.string(R.string.agent_tool_delete_schedule_event_confirm_title)

    override val confirmationToolPresentation: String =
        textProvider.string(R.string.agent_tool_delete_schedule_event_confirm_presentation)

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_delete_schedule_event_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_delete_schedule_event_examples)

    override val parametersSchema: ToolParameters = ToolParameters(
        properties = mapOf(
            "event_id" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_delete_schedule_event_param_event_id),
            ),
        ),
        required = listOf("event_id"),
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val json = if (arguments.isBlank()) JSONObject() else JSONObject(arguments)
            val eventId = json.optString("event_id").trim()
            require(eventId.isNotEmpty()) {
                textProvider.string(R.string.agent_tool_delete_schedule_event_id_required)
            }

            val event = service.getEvent(eventId)
                ?: error(textProvider.string(R.string.agent_tool_delete_schedule_event_not_found, eventId))
            service.deleteEventWithProviderBindings(eventId).getOrThrow()

            val deeplink = scheduleScreenDeeplink()
            scheduleSuccessResult(
                markdown = buildString {
                    appendLine("# ${sanitizeMarkdownText(title)}")
                    appendLine()
                    appendLine(
                        textProvider.string(
                            R.string.agent_tool_delete_schedule_event_deleted,
                            sanitizeMarkdownText(event.title),
                        )
                    )
                    appendLine()
                    appendLine("- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_schedule_open_link), deeplink)}")
                }.trimEnd(),
                deepLinks = listOf(
                    toolDeepLinkJson(
                        uri = deeplink,
                        label = textProvider.string(R.string.agent_tool_schedule_open_link),
                        primary = true,
                    )
                ),
            ) {
                put("eventId", eventId)
                put("deletedTitle", event.title)
            }
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_delete_schedule_event_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_schedule_unknown_error),
                ),
                isError = true,
            )
        }
    }
}
