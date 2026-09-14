package com.shifenmiao.base.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.t8rin.logger.makeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基础网络声音播放器。
 *
 * - 首次播放时将网络媒体文件下载到应用缓存目录；
 * - 后续相同 URL 直接复用本地缓存；
 * - 支持短音效播放和背景音乐循环播放；
 * - URL 可以指向常见音频文件，也可以指向包含音轨的视频文件。
 */
@Singleton
class NetworkAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).also { it.mkdirs() }
    }
    private val cacheMutex = Mutex()
    private var effectPlayer: MediaPlayer? = null
    private var backgroundPlayer: MediaPlayer? = null

    suspend fun playEffect(url: String) {
        val file = getCachedFile(url) ?: return
        withContext(Dispatchers.Main) {
            runCatching {
                effectPlayer?.release()
                effectPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener { player ->
                        player.release()
                        if (effectPlayer === player) effectPlayer = null
                    }
                    prepare()
                    start()
                }
            }.onFailure { it.makeLog("NetworkAudioPlayer") }
        }
    }

    suspend fun playBackground(url: String) {
        val file = getCachedFile(url) ?: return
        withContext(Dispatchers.Main) {
            runCatching {
                val currentSource = backgroundPlayerSource
                if (currentSource == url && backgroundPlayer?.isPlaying == true) return@runCatching
                stopBackground()
                backgroundPlayerSource = url
                backgroundPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
            }.onFailure { it.makeLog("NetworkAudioPlayer") }
        }
    }

    /**
     * 播放本地音频文件（用于 TTS 生成的本地缓存文件）。
     * [onComplete] 播放结束(或被打断释放)时回调。
     */
    suspend fun playLocalFile(file: File, onComplete: () -> Unit = {}) {
        if (!file.exists() || file.length() <= 0L) return
        withContext(Dispatchers.Main) {
            runCatching {
                effectPlayer?.release()
                effectPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener { player ->
                        player.release()
                        if (effectPlayer === player) effectPlayer = null
                        onComplete()
                    }
                    prepare()
                    start()
                }
            }.onFailure { it.makeLog("NetworkAudioPlayer") }
        }
    }

    /** 停止并释放当前短音频/本地文件播放(背景音乐走 [stopBackground]) */
    fun stopEffect() {
        runCatching {
            effectPlayer?.release()
        }
        effectPlayer = null
    }

    /**
     * 调整背景音播放速度（连带音高，1f 为原速）。
     *
     * 转盘转动声要跟着盘面一起慢下来，所以需要在播放过程中连续变速。
     * 变速走 MediaPlayer 的 PlaybackParams：底层是对解码后的 PCM 重采样，
     * 等效于磁带减速 —— 棘轮咔哒是宽带瞬态，音高下降不明显，但"节奏变慢"非常直观。
     *
     * 部分机型/解码器不支持变速，失败就静默维持原速，不影响转盘本身。
     */
    fun setBackgroundSpeed(speed: Float) {
        val player = backgroundPlayer ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val target = speed.coerceIn(0.1f, 2f)
        runCatching {
            val params = player.playbackParams
            // 差得少就不折腾，PlaybackParams 一写就要重采样，频繁设置有咔哒风险
            if (kotlin.math.abs(params.speed - target) > 0.01f) {
                params.speed = target
                player.playbackParams = params
            }
        }.onFailure { it.makeLog("NetworkAudioPlayer") }
    }

    /**
     * 淡出并停止背景音。
     *
     * 背景音是循环播放的(如转盘转动声),直接 [stopBackground] 会"啪"一下断掉,
     * 停在半句上很难听。这里用主线程的 Handler 分段压音量,淡出结束再释放。
     *
     * 淡出期间若已经换了别的背景音(引用变了),本次淡出立即让位给新音源。
     */
    fun fadeOutBackground(durationMs: Long = 400L) {
        val player = backgroundPlayer
        if (player == null || durationMs <= 0L) {
            stopBackground()
            return
        }
        val handler = Handler(Looper.getMainLooper())
        val steps = 10
        val stepMs = (durationMs / steps).coerceAtLeast(16L)
        var step = 0
        val ramp = object : Runnable {
            override fun run() {
                // 期间换了音源就别再压了,交给新音源自己处理
                if (backgroundPlayer !== player) return
                step++
                val volume = (1f - step.toFloat() / steps).coerceIn(0f, 1f)
                runCatching { player.setVolume(volume, volume) }
                if (step < steps) {
                    handler.postDelayed(this, stepMs)
                } else if (backgroundPlayer === player) {
                    // 淡出收尾才释放。期间要是已经转了下一轮(连点),新音源不能一起被掐掉
                    stopBackground()
                }
            }
        }
        handler.post(ramp)
    }

    fun stopBackground() {
        runCatching {
            backgroundPlayer?.release()
        }
        backgroundPlayer = null
        backgroundPlayerSource = null
    }

    suspend fun warmUp(url: String): Boolean {
        return getCachedFile(url) != null
    }

    private suspend fun getCachedFile(url: String): File? {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) return null
        return cacheMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    cacheDir.mkdirs()
                    val target = File(cacheDir, normalizedUrl.cacheFileName())
                    if (target.exists() && target.length() > 0L) return@runCatching target
                    val temp = File(cacheDir, "${target.name}.tmp")
                    URL(normalizedUrl).openStream().use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (temp.length() <= 0L) {
                        temp.delete()
                        return@runCatching null
                    }
                    if (target.exists()) target.delete()
                    temp.renameTo(target)
                    target
                }.onFailure { it.makeLog("NetworkAudioPlayer") }.getOrNull()
            }
        }
    }

    private fun String.cacheFileName(): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString(separator = "") { "%02x".format(it) }
        val extension = substringBefore('?')
            .substringAfterLast('/', missingDelimiterValue = "")
            .substringAfterLast('.', missingDelimiterValue = "")
            .takeIf { it.length in 2..6 }
            ?: DEFAULT_EXTENSION
        return "$hash.$extension"
    }

    private var backgroundPlayerSource: String? = null

    private companion object {
        const val CACHE_DIR_NAME = "network_audio"
        const val DEFAULT_EXTENSION = "media"
    }
}

