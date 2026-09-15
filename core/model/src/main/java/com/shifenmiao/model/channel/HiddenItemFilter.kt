package com.shifenmiao.model.channel

/**
 * 按渠道写死隐藏的服务端条目(item-list)。
 *
 * 服务端下发条目时无法按 flavor 过滤(Go 网关只按 versionCode / locale / vipLevel 筛),
 * 所以只能在客户端拦截: 同步落库前丢弃, 并把本地可能已有的残留一并删掉
 * (覆盖旧版本已经同步过该条目的情况)。
 *
 * 与 [NeedPrivacyPolicyDialog]、[PromptSensitiveCheckConfig] 同属"按渠道硬编码"的策略,
 * 区别是这里返回的是条目清单, 不是枚举开关。
 */
object HiddenItemFilter {

    /**
     * 「广告看看看」条目的 miniProgramId, 与 Screen.AdWatch.id 一致。
     *
     * 该页面的广告实现按 flavor 分: google 走 AdMob、国内渠道走 GroMore、
     * foss 是 NoopRewardedAdController(空实现)。foss 没有任何广告 SDK,
     * 入口点进去无广告可看, 故整条隐藏。
     */
    private const val AD_WATCH_ITEM_ID = "1095"

    /** 当前渠道要隐藏的条目 miniProgramId 集合 */
    fun hiddenMiniProgramIds(
        flavorType: FlavorType = FlavorType.fromName(),
    ): Set<String> = when (flavorType) {
        FlavorType.FOSS -> setOf(AD_WATCH_ITEM_ID)
        else -> emptySet()
    }

    /** 该条目在当前渠道是否应当隐藏 */
    fun isHidden(
        miniProgramId: String?,
        flavorType: FlavorType = FlavorType.fromName(),
    ): Boolean = !miniProgramId.isNullOrBlank() && miniProgramId in hiddenMiniProgramIds(flavorType)
}
