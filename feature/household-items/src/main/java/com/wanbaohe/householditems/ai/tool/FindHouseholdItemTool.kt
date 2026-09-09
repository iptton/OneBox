package com.wanbaohe.householditems.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import com.wanbaohe.householditems.R
import com.wanbaohe.householditems.service.HouseholdService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import org.json.JSONObject

/**
 * AI Agent 工具:`find_household_item` — 模糊查找物品,返回完整位置路径。
 *
 * 结果附家庭物品主页 deeplink,用户可点击直达。
 */
class FindHouseholdItemTool @Inject constructor(
    private val service: HouseholdService,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "find_household_item"

    override val description: String =
        textProvider.string(R.string.agent_tool_find_household_item_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_find_household_item_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_find_household_item_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SAFE

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_find_household_item_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_find_household_item_examples)

    override val requiresConfirmation: Boolean = false

    override val parametersSchema: ToolParameters = ToolParameters(
        type = "object",
        properties = mapOf(
            "query" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_find_household_item_param_query),
            ),
        ),
        required = listOf("query"),
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val json = if (arguments.isBlank()) JSONObject() else JSONObject(arguments)
            val query = json.optString("query").trim()
            require(query.isNotEmpty()) { "query 不能为空" }

            val items = service.searchItems(query)
            val deeplink = AddHouseholdItemTool.householdItemsDeeplink()
            AgentToolResult(
                content = buildString {
                    appendLine("# ${sanitizeMarkdownText(title)}")
                    appendLine()
                    if (items.isEmpty()) {
                        appendLine(textProvider.string(R.string.agent_tool_find_household_item_empty))
                    } else {
                        items.forEach { item ->
                            appendLine("- **${sanitizeMarkdownText(item.name)}**")
                            val path = item.locationPath.ifBlank {
                                textProvider.string(R.string.household_no_location)
                            }
                            appendLine("  - ${textProvider.string(R.string.agent_tool_household_label_location)}: ${sanitizeMarkdownText(path)}")
                            item.category?.takeIf { it.isNotBlank() }?.let {
                                appendLine("  - ${textProvider.string(R.string.agent_tool_household_label_category)}: ${sanitizeMarkdownText(it)}")
                            }
                            appendLine("  - ${textProvider.string(R.string.agent_tool_household_label_expiry)}: ${expiryText(item.expireAt)}")
                            item.note?.takeIf { it.isNotBlank() }?.let {
                                appendLine("  - ${textProvider.string(R.string.agent_tool_household_label_note)}: ${sanitizeMarkdownText(it)}")
                            }
                        }
                    }
                    appendLine()
                    appendLine("- ${buildMarkdownLink(textProvider.string(R.string.agent_tool_household_open_link), deeplink)}")
                }.trimEnd(),
            )
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_find_household_item_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_household_unknown_error),
                ),
                isError = true,
            )
        }
    }

    private fun expiryText(expireAt: Long?): String {
        if (expireAt == null) {
            return textProvider.string(R.string.agent_tool_household_expiry_none)
        }
        val date = Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
        return when {
            days < 0 -> textProvider.string(R.string.agent_tool_household_expiry_expired, -days)
            days == 0L -> textProvider.string(R.string.agent_tool_household_expiry_today)
            else -> textProvider.string(R.string.agent_tool_household_expiry_days_left, days)
        }
    }
}
