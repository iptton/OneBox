package com.wanbaohe.lifetime.ai.tool.di

import com.shifenmiao.ai.agent.tool.AgentTool
import com.wanbaohe.lifetime.ai.tool.AddCountdownEventTool
import com.wanbaohe.lifetime.ai.tool.AddLifeMilestoneTool
import com.wanbaohe.lifetime.ai.tool.DeleteLifetimeEntryTool
import com.wanbaohe.lifetime.ai.tool.QueryLifeEventsTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 人生时间线 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将倒数日/里程碑相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object LifetimeToolModule {

    @Provides
    @IntoMap
    @StringKey("add_countdown_event")
    fun provideAddCountdownEventTool(tool: AddCountdownEventTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("add_life_milestone")
    fun provideAddLifeMilestoneTool(tool: AddLifeMilestoneTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("query_life_events")
    fun provideQueryLifeEventsTool(tool: QueryLifeEventsTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("delete_lifetime_entry")
    fun provideDeleteLifetimeEntryTool(tool: DeleteLifetimeEntryTool): AgentTool = tool
}
