package com.shifenmiao.model.channel

/**
 * 国内合规要求下的隐私协议弹窗是否需要弹出（按渠道配置）。
 *
 * 国内应用市场审核要求首启弹隐私协议、登录前确认《用户协议》; 海外渠道(google / foss)无此要求,
 * 首启不弹窗、登录也不弹"服务协议及隐私保护"确认弹窗, 隐私政策链接保留在登录页/设置页即可。
 */
enum class NeedPrivacyPolicyDialog(val need: Boolean) {
    REQUIRED(true),
    NOT_REQUIRED(false);

    companion object {
        fun getConfigByFlavor(flavorType: FlavorType = FlavorType.fromName()): NeedPrivacyPolicyDialog {
            return when (flavorType) {
                FlavorType.GOOGLE, FlavorType.FOSS -> NOT_REQUIRED
                else -> REQUIRED
            }
        }
    }
}
