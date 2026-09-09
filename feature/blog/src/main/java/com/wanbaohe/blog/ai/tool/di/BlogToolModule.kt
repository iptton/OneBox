package com.wanbaohe.blog.ai.tool.di

import com.shifenmiao.ai.agent.tool.AgentTool
import com.wanbaohe.blog.ai.tool.SubmitFeedbackTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 博客/反馈 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将反馈相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object BlogToolModule {

    @Provides
    @IntoMap
    @StringKey("submit_feedback")
    fun provideSubmitFeedbackTool(tool: SubmitFeedbackTool): AgentTool = tool
}
