package com.wanbaohe.poem.ai.tool.di

import com.shifenmiao.ai.agent.tool.AgentTool
import com.wanbaohe.poem.ai.tool.GetPoemDetailTool
import com.wanbaohe.poem.ai.tool.QueryPoemFavoritesTool
import com.wanbaohe.poem.ai.tool.SearchPoemTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * 诗词 AI 工具注册模块。
 *
 * 通过 @IntoMap + @StringKey 将诗词相关工具注册到 AgentToolRegistry。
 */
@Module
@InstallIn(SingletonComponent::class)
object PoemToolModule {

    @Provides
    @IntoMap
    @StringKey("search_poem")
    fun provideSearchPoemTool(tool: SearchPoemTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("get_poem_detail")
    fun provideGetPoemDetailTool(tool: GetPoemDetailTool): AgentTool = tool

    @Provides
    @IntoMap
    @StringKey("query_poem_favorites")
    fun provideQueryPoemFavoritesTool(tool: QueryPoemFavoritesTool): AgentTool = tool
}
