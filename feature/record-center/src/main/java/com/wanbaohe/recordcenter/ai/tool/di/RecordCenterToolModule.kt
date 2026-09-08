package com.wanbaohe.recordcenter.ai.tool.di

import com.shifenmiao.ai.agent.tool.AgentTool
import com.wanbaohe.recordcenter.ai.tool.AddHealthRecordTool
import com.wanbaohe.recordcenter.ai.tool.GetHealthSummaryTool
import com.wanbaohe.recordcenter.ai.tool.QueryHealthRecordsTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 记录中心 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将健康记录相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object RecordCenterToolModule {

    @Provides
    @IntoMap
    @StringKey("add_health_record")
    fun provideAddHealthRecordTool(tool: AddHealthRecordTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("query_health_records")
    fun provideQueryHealthRecordsTool(tool: QueryHealthRecordsTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("get_health_summary")
    fun provideGetHealthSummaryTool(tool: GetHealthSummaryTool): AgentTool = tool
}
