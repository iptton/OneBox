package com.shifenmiao.common.ai

import com.google.gson.Gson
import com.shifenmiao.common.manager.AIEngineManager
import com.shifenmiao.model.ai.AiEngine
import com.shifenmiao.model.ai.AiRequestProtocol
import com.shifenmiao.model.ai.ChatCompletionChunk
import com.shifenmiao.model.ai.ChatCompletionRequest
import com.shifenmiao.model.ai.RequestMessage
import com.shifenmiao.model.ai.RoleType
import com.shifenmiao.network.AiRequestUrlResolver
import com.shifenmiao.network.api.OpenAICompatibleService
import com.shifenmiao.network.api.OwnProxyAIService
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.logger.makeLog
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局 AI 提示词执行器，输入系统提示词和用户内容即可同步获取当前工作引擎大模型的生成内容。
 *
 * 使用方式：
 * ```
 * @Inject lateinit var aiPromptExecutor: AIPromptExecutor
 *
 * // 在协程中调用（systemPrompt 可选，input 必填）
 * val result = aiPromptExecutor.execute(
 *     systemPrompt = "你是一个翻译助手，只输出翻译结果",
 *     input = "帮我翻译这段话：Hello World"
 * )
 * if (result.isSuccess) {
 *     val content = result.content  // AI 生成的内容
 * } else {
 *     val error = result.errorMessage  // 错误信息
 * }
 * ```
 */
@Singleton
class AIPromptExecutor @Inject constructor(
    private val aiEngineManager: AIEngineManager,
    private val openAICompatibleService: OpenAICompatibleService,
    private val ownProxyAIService: OwnProxyAIService,
    dispatchersHolder: DispatchersHolder,
) : DispatchersHolder by dispatchersHolder {

    private val gson = Gson()

    enum class EngineMode {
        DEFAULT,
        FAST,
        DUEL_A,
        DUEL_B,
    }

    suspend fun execute(
        input: String,
        systemPrompt: String = "",
        engineMode: EngineMode = EngineMode.DEFAULT,
    ): AIPromptResult {
        val engine = resolveEngine(engineMode)

        if (engine.name.isBlank()) {
            return AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = "AI engine not configured",
            )
        }

        // Phase 1 保护：本地引擎当前没有走 HTTP 链路的能力，
        // 直接在调用 URL Resolver 之前拦截，避免 error() 抛出崩溃。
        // Phase 2 计划：构造 LlmTurnRequest(stream = false) 走 LlmRequestGateway。
        if (engine.requestProtocol == AiRequestProtocol.LOCAL_ON_DEVICE) {
            return AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = "Local on-device engine is not yet supported by AIPromptExecutor (Phase 2)",
                engineName = engine.name,
                modelName = engine.model.name,
            )
        }

        val messages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(
                    RequestMessage.createTextMessage(
                        role = RoleType.SYSTEM.value,
                        text = systemPrompt
                    )
                )
            }
            add(
                RequestMessage.createTextMessage(
                    role = RoleType.USER.value,
                    text = input
                )
            )
        }

        val request = ChatCompletionRequest(
            model = engine.model.name,
            messages = messages,
            stream = false,
        )

        return try {
            val isProxyRoute = AiRequestUrlResolver.shouldUseProxyRequest(engine)
            val response = withContext(ioDispatcher) {
                val url = AiRequestUrlResolver.resolveRequestUrl(engine)
                if (AiRequestUrlResolver.shouldUseDirectRequest(engine)) {
                    val authorization = AiRequestUrlResolver.resolveAuthorizationHeader(engine)
                    openAICompatibleService.chatNoStreaming(
                        url = url,
                        authorization = authorization,
                        chatCompletionRequest = request
                    ).execute()
                } else {
                    ownProxyAIService.chatNoStreaming(
                        url = url,
                        chatCompletionRequest = request
                    ).execute()
                }
            }

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                makeLog { "AIPromptExecutor: HTTP ${response.code()} - $errorBody" }
                return AIPromptResult(
                    content = "",
                    isSuccess = false,
                    errorMessage = "HTTP ${response.code()}: ${errorBody.take(200)}",
                    engineName = engine.name,
                    modelName = engine.model.name,
                )
            }

            val body = response.body()?.string()
            if (body.isNullOrBlank()) {
                return AIPromptResult(
                    content = "",
                    isSuccess = false,
                    errorMessage = "Empty response body",
                    engineName = engine.name,
                    modelName = engine.model.name,
                )
            }

            val chunk = try {
                gson.fromJson(body, ChatCompletionChunk::class.java)
            } catch (e: Exception) {
                makeLog { "AIPromptExecutor: JSON parse failed: ${e.message}" }
                return AIPromptResult(
                    content = "",
                    isSuccess = false,
                    errorMessage = "Failed to parse response: ${e.message}",
                    engineName = engine.name,
                    modelName = engine.model.name,
                )
            }

            if (chunk.errorCode != 0 || chunk.errorMsg.isNotBlank()) {
                return AIPromptResult(
                    content = "",
                    isSuccess = false,
                    errorMessage = chunk.errorMsg.ifBlank { "API error code: ${chunk.errorCode}" },
                    engineName = engine.name,
                    modelName = engine.model.name,
                )
            }

            val content = chunk.choices.firstOrNull()?.message?.content.orEmpty()
            AIPromptResult(
                content = content,
                isSuccess = true,
                engineName = engine.name,
                modelName = engine.model.name,
                totalTokens = chunk.usage?.totalTokens ?: 0,
                isProxyRoute = isProxyRoute,
            )
        } catch (t: Throwable) {
            makeLog { "AIPromptExecutor: Request failed: ${t.message}" }
            AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = t.message ?: t.toString(),
                engineName = engine.name,
                modelName = engine.model.name,
            )
        }
    }

    /**
     * 流式变体：与 [execute] 参数一致，SSE 逐 chunk 回调 [onDelta]（参数为累计全文快照，
     * UI 直接整体替换即可），流结束后仍返回完整结果（成功时 content 为全文）。
     * [onReasoningDelta] 为深度思考内容( reasoning_content )的累计快照回调,仅作展示,不计入 content。
     */
    suspend fun executeStreaming(
        input: String,
        systemPrompt: String = "",
        engineMode: EngineMode = EngineMode.DEFAULT,
        onDelta: (String) -> Unit = {},
        onReasoningDelta: (String) -> Unit = {},
    ): AIPromptResult {
        val engine = resolveEngine(engineMode)

        if (engine.name.isBlank()) {
            return AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = "AI engine not configured",
            )
        }

        // 与 execute 一致:本地引擎当前没有走 HTTP 链路的能力,直接拦截
        if (engine.requestProtocol == AiRequestProtocol.LOCAL_ON_DEVICE) {
            return AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = "Local on-device engine is not yet supported by AIPromptExecutor (Phase 2)",
                engineName = engine.name,
                modelName = engine.model.name,
            )
        }

        val messages = buildList {
            if (systemPrompt.isNotBlank()) {
                add(
                    RequestMessage.createTextMessage(
                        role = RoleType.SYSTEM.value,
                        text = systemPrompt
                    )
                )
            }
            add(
                RequestMessage.createTextMessage(
                    role = RoleType.USER.value,
                    text = input
                )
            )
        }

        val request = ChatCompletionRequest(
            model = engine.model.name,
            messages = messages,
            stream = true,
        )

        return try {
            val isProxyRoute = AiRequestUrlResolver.shouldUseProxyRequest(engine)
            withContext(ioDispatcher) {
                val url = AiRequestUrlResolver.resolveRequestUrl(engine)
                val call = if (AiRequestUrlResolver.shouldUseDirectRequest(engine)) {
                    val authorization = AiRequestUrlResolver.resolveAuthorizationHeader(engine)
                    openAICompatibleService.chatWithStreaming(
                        url = url,
                        authorization = authorization,
                        chatCompletionRequest = request
                    )
                } else {
                    ownProxyAIService.chatWithStreaming(
                        url = url,
                        chatCompletionRequest = request
                    )
                }

                val content = StringBuilder()
                val reasoning = StringBuilder()
                var totalTokens = 0
                var errorMessage: String? = null
                try {
                    val response = call.execute()
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        makeLog { "AIPromptExecutor: stream HTTP ${response.code()} - $errorBody" }
                        errorMessage = "HTTP ${response.code()}: ${errorBody.take(200)}"
                    } else {
                        val body = response.body()
                        if (body == null) {
                            errorMessage = "Empty response body"
                        } else {
                            body.byteStream().bufferedReader().use { reader ->
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val line = reader.readLine() ?: break
                                    val trimmed = line.trim().trimStart('\uFEFF')
                                    if (!trimmed.startsWith("data:")) continue
                                    val payload = trimmed.substring("data:".length).trim()
                                    if (payload.isEmpty()) continue
                                    if (payload.equals("[DONE]", ignoreCase = true)) break
                                    val chunk = try {
                                        gson.fromJson(payload, ChatCompletionChunk::class.java)
                                    } catch (e: Exception) {
                                        // 单行解析失败属厂商兼容问题,丢弃该行即可
                                        makeLog { "AIPromptExecutor: drop malformed SSE line: ${e.message}" }
                                        continue
                                    }
                                    if (chunk.errorCode != 0 || chunk.errorMsg.isNotBlank()) {
                                        errorMessage =
                                            chunk.errorMsg.ifBlank { "API error code: ${chunk.errorCode}" }
                                        break
                                    }
                                    chunk.usage?.takeIf { it.totalTokens > 0 }
                                        ?.let { totalTokens = it.totalTokens }
                                    val choice = chunk.choices.firstOrNull()
                                    val reasoningDelta = choice?.delta?.reasoningContent?.takeIf { it.isNotEmpty() }
                                        ?: choice?.message?.reasoningContent
                                            ?.takeIf { choice.delta == null && it.isNotEmpty() }
                                    if (!reasoningDelta.isNullOrEmpty()) {
                                        reasoning.append(reasoningDelta)
                                        onReasoningDelta(reasoning.toString())
                                    }
                                    val delta = choice?.delta?.content?.takeIf { it.isNotEmpty() }
                                        ?: choice?.message?.content
                                            ?.takeIf { choice.delta == null && it.isNotEmpty() }
                                    if (!delta.isNullOrEmpty()) {
                                        content.append(delta)
                                        onDelta(content.toString())
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    runCatching { call.cancel() }
                }

                if (errorMessage != null) {
                    AIPromptResult(
                        content = "",
                        isSuccess = false,
                        errorMessage = errorMessage,
                        engineName = engine.name,
                        modelName = engine.model.name,
                    )
                } else {
                    AIPromptResult(
                        content = content.toString(),
                        isSuccess = true,
                        engineName = engine.name,
                        modelName = engine.model.name,
                        totalTokens = totalTokens,
                        isProxyRoute = isProxyRoute,
                    )
                }
            }
        } catch (t: Throwable) {
            makeLog { "AIPromptExecutor: Stream request failed: ${t.message}" }
            AIPromptResult(
                content = "",
                isSuccess = false,
                errorMessage = t.message ?: t.toString(),
                engineName = engine.name,
                modelName = engine.model.name,
            )
        }
    }

    private fun resolveEngine(engineMode: EngineMode): AiEngine {
        return when (engineMode) {
            EngineMode.DEFAULT -> aiEngineManager.getCurrentAiEngine()
            EngineMode.FAST -> aiEngineManager.getFastAiEngine()
            EngineMode.DUEL_A -> aiEngineManager.getDuelEngineA()
            EngineMode.DUEL_B -> aiEngineManager.getDuelEngineB()
        }
    }
}
