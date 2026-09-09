package com.wanbaohe.householditems.ai.tool

import com.shifenmiao.ai.agent.tool.AgentTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 家庭物品 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将家庭物品相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object HouseholdToolModule {

    @Provides
    @IntoMap
    @StringKey("add_household_item")
    fun provideAddHouseholdItemTool(tool: AddHouseholdItemTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("find_household_item")
    fun provideFindHouseholdItemTool(tool: FindHouseholdItemTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("move_household_item")
    fun provideMoveHouseholdItemTool(tool: MoveHouseholdItemTool): AgentTool = tool
}
