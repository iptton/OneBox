package com.wanbaohe.unitconverter.domain

import androidx.annotation.StringRes

/**
 * 亲属计算结果。
 * title 以资源 ID 形式返回，由 UI 层解析为本地化文案。
 */
sealed class KinshipTitle {
    data class Fixed(@StringRes val resId: Int) : KinshipTitle()
    data class ByGender(
        @StringRes val maleResId: Int,
        @StringRes val femaleResId: Int,
    ) : KinshipTitle()

    /** 映射未命中时，用链路步骤拼出描述性称谓 */
    data class Descriptive(val steps: List<KinshipStep>) : KinshipTitle()
    object Empty : KinshipTitle()
}

data class KinshipResult(
    val title: KinshipTitle,
    val steps: List<KinshipStep>,
)
