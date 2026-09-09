package com.shifenmiao.ai.agent.tool.builtin

import com.shifenmiao.ai.R
import com.shifenmiao.ai.agent.tool.AgentTool
import com.shifenmiao.ai.agent.tool.AgentToolResult
import com.shifenmiao.ai.agent.tool.AgentToolTextProvider
import com.shifenmiao.base.audio.NetworkAudioPlayer
import com.shifenmiao.model.ai.ToolParameters
import com.shifenmiao.model.ai.tool.ToolCategory
import com.shifenmiao.model.ai.tool.ToolRiskLevel
import javax.inject.Inject

/**
 * 内置工具:`stop_reading` — 停止当前正在进行的朗读。
 *
 * 停止 core/base NetworkAudioPlayer 的短音频播放(read_text_aloud 走的正是该通道),
 * 不影响背景音乐。
 */
class StopReadingTool @Inject constructor(
    private val audioPlayer: NetworkAudioPlayer,
    private val textProvider: AgentToolTextProvider,
) : AgentTool {

    override val name: String = "stop_reading"

    override val description: String =
        textProvider.string(R.string.agent_tool_stop_reading_description)

    override val title: String =
        textProvider.string(R.string.agent_tool_stop_reading_title)

    override val summary: String =
        textProvider.string(R.string.agent_tool_stop_reading_summary)

    override val category: ToolCategory = ToolCategory.MEDIA

    override val riskLevel: ToolRiskLevel = ToolRiskLevel.SAFE

    // 停止操作作用于全局音频播放器,与朗读操作互斥,不可并行
    override val parallelizable: Boolean = false

    override val keywords: List<String> =
        textProvider.array(R.array.agent_tool_stop_reading_keywords)

    override val examples: List<String> =
        textProvider.array(R.array.agent_tool_stop_reading_examples)

    override val parametersSchema: ToolParameters = ToolParameters()

    override suspend fun execute(arguments: String): AgentToolResult {
        audioPlayer.stopEffect()
        return AgentToolResult(
            content = textProvider.string(R.string.agent_tool_stop_reading_stopped)
        )
    }
}
