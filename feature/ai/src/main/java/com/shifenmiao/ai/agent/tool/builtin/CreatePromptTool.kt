package com.shifenmiao.ai.agent.tool.builtin

import com.google.gson.Gson
import com.shifenmiao.ai.R
import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.ai.service.PromptCreationService
import com.shifenmiao.model.ai.ToolParameterProperty
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import org.json.JSONObject
import javax.inject.Inject

/**
 * 提示词创建工具 — 根据需求描述用 AI 生成并保存一条本地提示词
 *
 * 复用创建提示词页的底层服务 [PromptCreationService.createAndSaveFromRequirement]:
 * 一次性 AI 调用生成完整提示词模板（标题/说明/系统提示词/占位提示）→ 写入本地 Room
 * （item + prompt + 分类关联，自动置顶），与人工创建页面保存的数据结构完全一致。
 */
class CreatePromptTool @Inject constructor(
    private val promptCreationService: PromptCreationService,
    private val textProvider: AgentToolTextProvider,
    private val gson: Gson
) : AgentTool {
    override val name = "create_prompt"
    override val description = textProvider.string(R.string.agent_tool_create_prompt_description)
    override val title: String = textProvider.string(R.string.agent_tool_create_prompt_title)
    override val summary: String = textProvider.string(R.string.agent_tool_create_prompt_summary)
    override val examples: List<String> = textProvider.array(R.array.agent_tool_create_prompt_examples)
    override val category: ToolCategory = ToolCategory.PROMPT
    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SENSITIVE
    override val sortOrder: Int = -69

    // 内部还有一次性的 AI 生成调用，比纯本地操作耗时更长
    override val executionTimeoutMs: Long = 120_000L

    override val parametersSchema = ToolParameters(
        properties = mapOf(
            "requirement" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_create_prompt_param_requirement)
            ),
            "category" to ToolParameterProperty(
                type = "string",
                description = textProvider.string(R.string.agent_tool_create_prompt_param_category)
            )
        ),
        required = listOf("requirement")
    )

    override suspend fun execute(arguments: String): AgentToolResult {
        return try {
            val json = JSONObject(arguments)
            val requirement = json.optString("requirement").trim()
            if (requirement.isEmpty()) {
                return AgentToolResult(
                    content = textProvider.string(R.string.agent_tool_create_prompt_requirement_required),
                    isError = true
                )
            }
            val category = json.optString("category").trim()

            val result = promptCreationService.createAndSaveFromRequirement(
                userGoal = requirement,
                categoryHints = listOfNotNull(category.takeIf { it.isNotEmpty() })
            )

            AgentToolResult(
                content = gson.toJson(
                    CreatePromptResponse(
                        tool = name,
                        success = true,
                        promptId = result.prompt.id,
                        itemId = result.itemId,
                        title = result.prompt.title.orEmpty(),
                        categoryNames = result.selectedCategoryNames,
                        message = textProvider.string(
                            R.string.agent_tool_create_prompt_success,
                            result.prompt.title.orEmpty(),
                            result.prompt.id,
                            result.selectedCategoryNames.joinToString()
                        )
                    )
                )
            )
        } catch (e: Exception) {
            AgentToolResult(
                content = textProvider.string(
                    R.string.agent_tool_create_prompt_failed,
                    e.message ?: textProvider.string(R.string.agent_tool_unknown_error)
                ),
                isError = true
            )
        }
    }

    private data class CreatePromptResponse(
        val tool: String,
        val success: Boolean,
        val promptId: Int,
        val itemId: Int,
        val title: String,
        val categoryNames: List<String>,
        val message: String
    )
}
