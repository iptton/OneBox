package com.wanbaohe.schedule.ai.tool.di

import com.shifenmiao.ai.agent.tool.AgentTool
import com.wanbaohe.schedule.ai.tool.AddScheduleEventTool
import com.wanbaohe.schedule.ai.tool.DeleteScheduleEventTool
import com.wanbaohe.schedule.ai.tool.QueryScheduleEventsTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 日程 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将日程相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object ScheduleToolModule {

    @Provides
    @IntoMap
    @StringKey("add_schedule_event")
    fun provideAddScheduleEventTool(tool: AddScheduleEventTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("query_schedule_events")
    fun provideQueryScheduleEventsTool(tool: QueryScheduleEventsTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("delete_schedule_event")
    fun provideDeleteScheduleEventTool(tool: DeleteScheduleEventTool): AgentTool = tool
}
