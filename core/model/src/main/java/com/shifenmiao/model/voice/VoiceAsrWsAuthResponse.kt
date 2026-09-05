package com.shifenmiao.model.voice

import kotlinx.serialization.Serializable

/**
 * 语音输入 WebSocket 鉴权响应。
 *
 * 由 Go 网关 `GET api/voice/asr/ws-auth` 下发：服务端持有讯飞密钥并签名，
 * App 只拿到一次性签名 wss URL 直连讯飞，密钥不出服务端。
 *
 * @param url 已签名的讯飞 WebSocket 地址
 * @param appId 讯飞应用 appid,首帧 header 需要
 * @param expiresIn 签名有效期(秒)
 */
@Serializable
data class VoiceAsrWsAuthResponse(
    val url: String,
    val appId: String,
    val expiresIn: Int
)
