package com.shifenmiao.ai.voice

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 自建语音识别客户端(FunASR 实时 2pass, 经 Go 网关 /api/voice/asr/ws 反代)。
 *
 * 与讯飞协议的区别: 音频不再 base64 塞进 JSON, 而是首帧发参数、之后直接发二进制 PCM。
 * 上行: {"mode":"2pass","is_speaking":true,"wav_format":"pcm","chunk_size":[5,10,5],...}
 *      -> 16k 单声道 PCM 二进制帧 -> {"is_speaking":false}
 * 下行: mode=2pass-online 为边说边出的中间结果; mode=2pass-offline 为句尾经高精度模型
 *      修正后的结果(带标点), 收到它即视为最终文本。
 * 热词不用客户端传: 网关按 go-config 的 voice_asr.self_hotwords 在首帧注入。
 */
class FunAsrClient(
    private val state: MutableStateFlow<AsrState>,
) : AsrStreamClient {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val sendQueue = LinkedBlockingQueue<ByteArray>()
    private var senderThread: Thread? = null

    /** 已定稿的句段(2pass-offline 结果)与当前中间结果, 拼成完整文本 */
    @Volatile
    private var finalText = ""

    @Volatile
    private var partialText = ""

    @Volatile
    private var endRequested = false

    @Volatile
    private var cancelled = false

    /** 连接网关并开始识别。token 由调用方从本地登录态取出(网关 JRW 鉴权)。 */
    fun connect(wsUrl: String, token: String?) {
        val builder = Request.Builder().url(wsUrl)
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        webSocket = httpClient.newWebSocket(builder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                startSender()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!cancelled && state.value !is AsrState.Finished) {
                    state.value = AsrState.Error(t.message ?: "WebSocket failure")
                }
            }
        })
    }

    override fun sendPcm(chunk: ByteArray) {
        if (!cancelled) sendQueue.offer(chunk)
    }

    override fun stop() {
        endRequested = true
    }

    override fun cancel() {
        cancelled = true
        senderThread?.interrupt()
        sendQueue.clear()
        runCatching { webSocket?.cancel() }
        webSocket = null
        httpClient.dispatcher.executorService.shutdown()
    }

    /* ─────────── private ─────────── */

    private fun startSender() {
        senderThread = Thread({
            var started = false
            try {
                while (!cancelled) {
                    val chunk = sendQueue.poll(200, TimeUnit.MILLISECONDS)
                    if (chunk != null) {
                        if (!started) {
                            sendText(buildStartFrame())
                            started = true
                        }
                        webSocket?.send(chunk.toByteString())
                        // 录音天然 ~40ms/块, 这里兜底节流, 防止网关/WSS 慢于录音时堆积
                        Thread.sleep(FRAME_INTERVAL_MS)
                    } else if (endRequested) {
                        if (!started) sendText(buildStartFrame())
                        sendText(END_FRAME)
                        scheduleFallbackFinish()
                        break
                    }
                }
            } catch (_: InterruptedException) {
            }
        }, "funasr-asr-sender").also { it.start() }
    }

    private fun sendText(frame: String) {
        runCatching { webSocket?.send(frame) }
    }

    /** 首帧参数: 与 FunASR 2pass 协议一致; chunk_size [5,10,5] = 600ms 出一次中间结果 */
    private fun buildStartFrame(): String = gson.toJson(
        mapOf(
            "mode" to "2pass",
            "wav_name" to "onebox",
            "is_speaking" to true,
            "wav_format" to "pcm",
            "audio_fs" to PcmAudioRecorder.SAMPLE_RATE,
            "chunk_size" to listOf(5, 10, 5),
            "itn" to true,
            "svs_itn" to true
        )
    )

    /** 兜底: 结束帧发出后若服务端迟迟不回最终结果, 用当前文本收尾, 避免面板卡住 */
    private fun scheduleFallbackFinish() {
        Thread({
            try {
                Thread.sleep(FINISH_FALLBACK_MS)
            } catch (_: InterruptedException) {
                return@Thread
            }
            if (!cancelled && state.value !is AsrState.Finished) {
                val text = joinedText()
                if (text.isNotBlank()) state.value = AsrState.Finished(text)
            }
        }, "funasr-finish-fallback").start()
    }

    private fun handleMessage(text: String) {
        // 解析不了的帧直接忽略, 不 crash
        try {
            val root = JsonParser.parseString(text).asJsonObject
            val mode = root.get("mode")?.asString.orEmpty()
            val recognized = root.get("text")?.asString.orEmpty()
            if (mode.contains("offline")) {
                if (recognized.isNotBlank()) finalText += recognized
                partialText = ""
                val joined = joinedText()
                state.value = if (endRequested) {
                    if (joined.isBlank()) AsrState.Listening("") else AsrState.Finished(joined)
                } else {
                    AsrState.Listening(joined)
                }
            } else {
                partialText = recognized
                state.value = AsrState.Listening(joinedText())
            }
        } catch (_: Exception) {
        }
    }

    private fun joinedText(): String = finalText + partialText

    private companion object {
        /** 与讯飞侧一致的 40ms/帧节流 */
        const val FRAME_INTERVAL_MS = 40L

        /** 结束帧后等待最终结果的兜底时长 */
        const val FINISH_FALLBACK_MS = 3000L

        const val END_FRAME = "{\"is_speaking\":false}"
    }
}
