package com.shifenmiao.ai.voice

import com.shifenmiao.network.api.ApiService
import com.shifenmiao.storage.RemoteConfigStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音输入识别器(讯飞大模型识别)。
 *
 * 流程:[prepare] 获取签名 wss URL + appId(RemoteConfig 下发了凭据则本地签名,
 * 否则经 Go 网关代签,密钥不出服务端)→
 * [start] 建 WebSocket 客户端 + PCM 录音器开始流式识别 →
 * [finish] 用户主动说完(发结束帧,等最终结果)或 [cancel] 放弃。
 * 识别状态经 [state] 输出,录音音量经 [rms] 输出供 UI 脉冲动画。
 */
@Singleton
class VoiceRecognizer @Inject constructor(
    private val apiService: ApiService
) {

    private val _state = MutableStateFlow<AsrState>(AsrState.Idle)

    /** 识别状态:Idle / Listening(流式中间结果) / Finished / Error */
    val state: StateFlow<AsrState> = _state.asStateFlow()

    private val _rms = MutableStateFlow(0f)

    /** 当前录音音量 0..1 */
    val rms: StateFlow<Float> = _rms.asStateFlow()

    private var client: IFlytekAsrClient? = null
    private var recorder: PcmAudioRecorder? = null
    private var timeoutJob: Job? = null

    /**
     * 获取 {签名 wss URL, appId}。
     * 联调期:RemoteConfig 下发了完整讯飞凭据(appId/apiKey/apiSecret)时本地签名直连;
     * 否则向 Go 网关代签(/api/voice/asr/ws-auth,密钥不出服务端)。失败返回 null。
     */
    suspend fun prepare(): Pair<String, String>? {
        val local = RemoteConfigStorage.getRemoteConfig().voiceInput
        if (!local?.appId.isNullOrBlank() &&
            !local?.apiKey.isNullOrBlank() &&
            !local?.apiSecret.isNullOrBlank()
        ) {
            return IFlytekAuth.buildWsUrl(local!!.apiKey!!, local.apiSecret!!) to local.appId!!
        }
        return try {
            val response = apiService.getVoiceAsrWsAuth()
            response.body()
                ?.takeIf { response.isSuccessful && it.url.isNotBlank() }
                ?.let { it.url to it.appId }
        } catch (_: Exception) {
            null
        }
    }

    /** 开始识别。防重入:先 cancel 掉上一轮。60s 上限到点自动 finish。 */
    fun start(url: String, appId: String, scope: CoroutineScope) {
        cancel()
        _state.value = AsrState.Listening("")
        val newClient = IFlytekAsrClient(appId = appId, state = _state)
        client = newClient
        newClient.connect(url)
        val newRecorder = PcmAudioRecorder()
        recorder = newRecorder
        try {
            newRecorder.start(
                onChunk = { newClient.sendPcm(it) },
                onRms = { _rms.value = it.toFloat() }
            )
        } catch (e: Exception) {
            cancel()
            _state.value = AsrState.Error(e.message ?: "recorder start failed")
            return
        }
        timeoutJob = scope.launch {
            delay(MAX_DURATION_MS)
            finish()
        }
    }

    /** 用户主动说完:停录音、发结束帧,最终结果仍经 [state] 回调 */
    fun finish() {
        timeoutJob?.cancel()
        timeoutJob = null
        recorder?.stop()
        recorder = null
        client?.stop()
        client = null
    }

    /** 放弃:立即断开并复位状态 */
    fun cancel() {
        timeoutJob?.cancel()
        timeoutJob = null
        recorder?.stop()
        recorder = null
        client?.cancel()
        client = null
        _rms.value = 0f
        _state.value = AsrState.Idle
    }

    companion object {
        /** 单次识别上限 60s */
        private const val MAX_DURATION_MS = 60_000L
    }
}
