package com.shifenmiao.ai.agent.tool

import com.google.gson.Gson
import com.google.gson.JsonParser

object InteractiveToolResultFactory {

    fun buildQuestionSubmittedResult(
        answersJson: String,
        gson: Gson
    ): AgentToolResult {
        return AgentToolResult(
            content = gson.toJson(
                mapOf(
                    "status" to "submitted",
                    "answers" to gson.fromJson(answersJson, Map::class.java).orEmpty()
                )
            ),
            isError = false
        )
    }

    fun buildQuestionCancelledResult(gson: Gson): AgentToolResult {
        return AgentToolResult(
            content = gson.toJson(
                mapOf(
                    "status" to "cancelled"
                )
            ),
            isError = false
        )
    }

    fun buildConfirmationRejectedResult(
        toolName: String,
        reason: String,
        gson: Gson
    ): AgentToolResult {
        return AgentToolResult(
            content = gson.toJson(
                mapOf(
                    "toolName" to toolName,
                    "decision" to "rejected",
                    "executed" to false,
                    "message" to "用户拒绝了本次工具执行",
                    "reason" to reason
                )
            ),
            isError = false
        )
    }

    /**
     * 判定确认弹窗结果是否为"已批准".
     *
     * 确认结果的实际 JSON 结构由发起方 payload 决定
     * (见 AgentLoopExecutor.requestToolConfirmation):
     * - 批准: `{"decision":"approved"}`
     * - 拒绝: `{"decision":"rejected"}`
     *
     * 解析失败、结构异常或字段缺失一律按"未批准"处理 (安全方向);
     * 同时兼容历史上可能出现过的 `{"approved":true}` 形式.
     */
    fun isConfirmationApproved(payload: String?): Boolean {
        if (payload.isNullOrBlank()) return false
        return runCatching {
            val obj = JsonParser.parseString(payload).asJsonObject
            obj.get("decision")?.takeIf { it.isJsonPrimitive }?.asString == "approved" ||
                obj.get("approved")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
        }.getOrDefault(false)
    }
}
