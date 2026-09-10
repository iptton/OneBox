package com.wanbaohe.period.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
object PeriodToolModule {

    @Provides
    @IntoMap
    @StringKey("add_period_record")
    fun provideAddPeriodRecordTool(tool: AddPeriodRecordTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("query_period_records")
    fun provideQueryPeriodRecordsTool(tool: QueryPeriodRecordsTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("update_period_record")
    fun provideUpdatePeriodRecordTool(tool: UpdatePeriodRecordTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("delete_period_record")
    fun provideDeletePeriodRecordTool(tool: DeletePeriodRecordTool): AgentTool = tool
}
