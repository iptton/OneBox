package com.shifenmiao.model.user.event

/**
 * 请求用户完成联系方式验证(邮箱或手机号)的事件。
 * AI 功能发送前拦截: 未验证用户触发本事件, 由全局 VerifyContactSheet 引导验证。
 */
data class VerifyContactEvent(
    var source: String = "VerifyContactEvent",
    var onSuccess: () -> Unit = {},
)
