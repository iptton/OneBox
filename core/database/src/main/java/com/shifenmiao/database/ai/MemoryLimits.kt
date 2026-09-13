package com.shifenmiao.database.ai

/**
 * AI 记忆的容量与注入策略常量（feature/ai 与 feature/settings 共用）。
 */
object MemoryLimits {

    /** log 条目滚动淘汰上限（profile 不参与） */
    const val MAX_LOG_ENTRIES = 500

    /** 关键词搜索返回上限 */
    const val SEARCH_RESULT_LIMIT = 60

    /** 无关键词 dump 返回上限 */
    const val DUMP_RESULT_LIMIT = 500

    /** 搜索 / dump 总输出字节上限（UTF-8） */
    const val MAX_OUTPUT_BYTES = 30 * 1024

    /** 注入的 log 日期桶数量（最近 N 个有内容的日期，不是近 N 天） */
    const val PROMPT_LOG_BUCKETS = 3

    /** 记忆 fragment 占 prompt 预算的比例上限 */
    const val PROMPT_BUDGET_FRACTION = 0.15

    /** 记忆 fragment 的 token 预算下限（小窗口模型也保留基本注入能力） */
    const val MIN_PROMPT_BUDGET_TOKENS = 800

    /** 单条记忆内容字符上限（memory_write 工具与管理页手动新增统一） */
    const val MAX_ENTRY_CHARS = 2000
}
