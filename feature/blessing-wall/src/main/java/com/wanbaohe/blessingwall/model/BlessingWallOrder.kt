package com.wanbaohe.blessingwall.model

import com.shifenmiao.model.channel.FlavorType

/**
 * 祈福墙 tab 展示顺序。
 * - 国内渠道：传统祈福在前，国际主题在后。
 * - 海外渠道（google / foss）：守护天使、招财猫优先，其余国际主题在前。
 */
object BlessingWallOrder {

    private val domestic = listOf(
        BlessingType.WOODEN_FISH,
        BlessingType.WEALTH_GOD,
        BlessingType.GUANYIN,
        BlessingType.INCENSE,
        BlessingType.HAMSA,
        BlessingType.CUPID,
        BlessingType.CLOVER,
        BlessingType.ANGEL,
        BlessingType.LUCKY_CAT,
        BlessingType.DREAMCATCHER,
        BlessingType.WISHBONE,
        BlessingType.HORSESHOE,
    )

    private val overseas = listOf(
        BlessingType.ANGEL,
        BlessingType.LUCKY_CAT,
        BlessingType.HAMSA,
        BlessingType.CUPID,
        BlessingType.CLOVER,
        BlessingType.DREAMCATCHER,
        BlessingType.WISHBONE,
        BlessingType.HORSESHOE,
        BlessingType.WOODEN_FISH,
        BlessingType.WEALTH_GOD,
        BlessingType.GUANYIN,
        BlessingType.INCENSE,
    )

    fun types(
        isOverseas: Boolean = FlavorType.fromName().isOverseas,
    ): List<BlessingType> = if (isOverseas) overseas else domestic

    fun indexOf(
        type: BlessingType?,
        isOverseas: Boolean = FlavorType.fromName().isOverseas,
    ): Int {
        if (type == null) return 0
        return types(isOverseas).indexOf(type).coerceAtLeast(0)
    }

    fun indexOfKey(
        key: String?,
        isOverseas: Boolean = FlavorType.fromName().isOverseas,
    ): Int = indexOf(BlessingType.fromKey(key.orEmpty()), isOverseas)
}
