package com.wanbaohe.adwatch.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** 激励广告加载状态 */
enum class RewardedAdStatus {
    /** 空闲(尚未请求加载) */
    IDLE,

    /** 加载中 */
    LOADING,

    /** 已就绪, 可展示 */
    READY,

    /** 加载或展示失败 */
    FAILED,

    /** 当前渠道不支持广告(非 google 渠道 stub) */
    UNAVAILABLE,
}

object AdWatchAds {

    /** AdMob 正式激励广告单元 ID(AdMob 后台「Ad Watch 激励广告」单元) */
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-2510840726350411/5526322638"

    /** Google 官方激励广告测试单元 ID,开发调试时可临时换回 */
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /** 每日观看上限次数 */
    const val DAILY_LIMIT = 10

    /** 远程配置未下发时的默认单次奖励积分 */
    const val DEFAULT_REWARD_POINTS = 50
}

/**
 * 激励广告控制器。
 *
 * main 源集只声明接口, 不 import 任何 com.google.android.gms.ads 类:
 *   - google 渠道: src/google 的 GmsRewardedAdController (真实 AdMob 实现)
 *   - 国内渠道 + foss: src/nogms 的 NoopRewardedAdController (不可用 stub)
 * 两侧各有一个同签名的 Hilt Module 负责绑定, 互斥编译。
 */
interface RewardedAdController {

    /** 当前广告加载状态 */
    val status: StateFlow<RewardedAdStatus>

    /** 当前渠道是否支持激励广告(仅 google 渠道为 true) */
    val isSupported: Boolean

    /** 预加载一条激励广告; 已有就绪广告或正在加载时不重复请求 */
    fun preload()

    /**
     * 展示激励广告。
     *
     * onEarned: 用户看完广告获得奖励(此时应发放积分);
     * onClosed: 广告关闭(无论是否获得奖励, 内部随后会自动预加载下一条);
     * onError: 广告未就绪或展示失败。
     */
    fun show(
        activity: Activity,
        onEarned: () -> Unit,
        onClosed: () -> Unit,
        onError: () -> Unit,
    )
}
