package com.wanbaohe.aidetect.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * AI 检测助手 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将 AIGC 检测工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object AiDetectToolModule {

    @Provides
    @IntoMap
    @StringKey("detect_ai_text")
    fun provideDetectAiTextTool(tool: DetectAiTextTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("detect_ai_image")
    fun provideDetectAiImageTool(tool: DetectAiImageTool): AgentTool = tool
}
