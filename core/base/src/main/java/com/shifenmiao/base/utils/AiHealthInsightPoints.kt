package com.shifenmiao.base.utils

import com.shifenmiao.storage.RemoteConfigStorage

/**
 * AI 解读类能力(如健康记录「AI 解读」)单次积分成本:
 * 远程配置(RemoteConfig.aiHealthInsightPoints)可动态调整,
 * 未下发时回退默认 [DEFAULT_AI_HEALTH_INSIGHT_POINTS]。
 *
 * 与 [aiImageProcessPointsCost] 同款约定;调用方自行定义积分来源 source。
 */
fun aiHealthInsightPointsCost(): Int =
    RemoteConfigStorage.getRemoteConfig().aiHealthInsightPoints ?: DEFAULT_AI_HEALTH_INSIGHT_POINTS

/** AI 解读默认单次积分成本(远程未下发时) */
private const val DEFAULT_AI_HEALTH_INSIGHT_POINTS = 200
