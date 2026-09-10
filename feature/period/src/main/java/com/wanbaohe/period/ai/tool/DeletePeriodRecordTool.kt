package com.wanbaohe.period.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.base.utils.ActionUtils
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import com.wanbaohe.period.R
import com.wanbaohe.period.service.PeriodService
import org.json.JSONObject
import javax.inject.Inject
class DeletePeriodRecordTool @Inject constructor(
    private val service: PeriodService,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "delete_period_record"

    override val description: String =
        textProvider.string(R.string.agent_tool_delete_period_record_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_delete_period_record_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_delete_period_record_summary)

    override val category: ToolCategory = ToolCategory.BUSINESS

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.DANGEROUS

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_delete_period_record_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_delete_period_record_examples)

    override val requiresConfirmation: Boolean = true

    override val parametersSchema: ToolParameters = ToolParameters(
        type = "object",
        properties = mapOf(
            "record_id" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_period_param_record_id),
            ),
        ),
        required = listOf("record_id"),
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return runCatching {
            val json = if (arguments.isBlank()) JSONObject() else JSONObject(arguments)
            val recordId = json.optString("record_id").trim()
            require(recordId.isNotEmpty()) { "record_id 不能为空" }
            val existing = service.getRecordUi(recordId)
            service.deleteRecord(
                recordId = recordId,
                actor = PeriodService.ACTOR_AGENT,
                source = PeriodService.SOURCE_AGENT_DELETE,
            ).getOrThrow()
            ActionUtils.showToast(R.string.period_deleted)

            AgentToolResult(
                content = buildString {
                    appendLine("# ${sanitizeMarkdownText(title)}")
                    appendLine()
                    appendLine(
                        textProvider.string(
                            R.string.agent_tool_delete_period_record_done,
                            existing?.recordDate?.toString() ?: recordId,
                        )
                    )
                    appendLine()
                    appendLine(
                        "- ${buildMarkdownLink(
                            textProvider.string(R.string.agent_tool_period_open_link),
                            AddPeriodRecordTool.periodListDeeplink(),
                        )}"
                    )
                }.trimEnd(),
            )
        }.getOrElse { error ->
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_delete_period_record_failed,
                    error.message ?: textProvider.string(R.string.agent_tool_period_unknown_error),
                ),
                isError = true,
            )
        }
    }
}
