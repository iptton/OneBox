package com.wanbaohe.adwatch.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import com.shifenmiao.base.utils.CoreUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

// TODO(穿山甲): 联调用官方测试 appId/代码位; 上线前替换为穿山甲后台「万宝盒」应用的正式值
/** 穿山甲官方测试应用 ID (csjplatform 测试 demo 通用) */
private const val CSJ_TEST_APP_ID = "5001121"

/** 穿山甲官方测试激励视频代码位 (Android, 与测试 appId 5001121 配套) */
private const val CSJ_TEST_REWARD_CODE_ID = "901121365"

/**
 * 国内渠道真实实现: 封装穿山甲 (Pangle 国内版) SDK 懒初始化 + 激励视频加载/展示。
 * 仅 src/domestic 编译, google 渠道见 src/google, foss 见 src/nogms 的同签名 stub。
 *
 * 合规红线: 用户未同意隐私协议前不得调 TTAdSdk.init/start,
 * 首次 preload 时才检查并初始化 (正常路径入口页已在隐私弹窗之后, 此处为防御性跳过)。
 */
@Singleton
class CsjRewardedAdController @Inject constructor(
    @ApplicationContext private val context: Context,
) : RewardedAdController {

    private val _status = MutableStateFlow(RewardedAdStatus.IDLE)
    override val status: StateFlow<RewardedAdStatus> = _status.asStateFlow()

    override val isSupported: Boolean = true

    private var rewardedAd: TTRewardVideoAd? = null
    private val sdkStarted = AtomicBoolean(false)
    private var sdkStarting = false
    private var loading = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private companion object {
        const val TAG = "CsjRewardedAd"
    }

    override fun preload() {
        if (rewardedAd != null || loading) return
        // 隐私协议未同意: 不触碰穿山甲 SDK, 状态置 FAILED 供页面展示重试
        if (CoreUtils.isShowPrivacyPolicyDialog()) {
            _status.value = RewardedAdStatus.FAILED
            return
        }
        // 穿山甲要求 init/start 在主线程调用
        mainHandler.post { ensureSdkStarted { loadAd() } }
    }

    /** init 一次 + start 回调成功后执行 onReady */
    private fun ensureSdkStarted(onReady: () -> Unit) {
        if (sdkStarted.get()) {
            onReady()
            return
        }
        if (sdkStarting) return
        sdkStarting = true
        // init 幂等 (SDK 文档: 多次初始化以第一次配置为准), 无需先查 isInitSuccess (已弃用)
        TTAdSdk.init(context, buildAdConfig())
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                sdkStarting = false
                sdkStarted.set(true)
                onReady()
            }

            override fun fail(code: Int, msg: String?) {
                Log.w(TAG, "TTAdSdk.start fail: code=$code msg=$msg")
                sdkStarting = false
                _status.value = RewardedAdStatus.FAILED
            }
        })
    }

    private fun buildAdConfig(): TTAdConfig =
        TTAdConfig.Builder()
            .appId(CSJ_TEST_APP_ID)
            .appName(context.applicationInfo.loadLabel(context.packageManager).toString())
            .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
            .allowShowNotify(true)
            .debug(false)
            .supportMultiProcess(false)
            // 合规: 关闭位置/应用列表/电话状态/WiFi 状态/外部存储等敏感采集
            .customController(object : TTCustomController() {
                override fun isCanUseLocation(): Boolean = false
                override fun alist(): Boolean = false
                override fun isCanUsePhoneState(): Boolean = false
                override fun isCanUseWifiState(): Boolean = false
                override fun isCanUseWriteExternal(): Boolean = false
            })
            .build()

    private fun loadAd() {
        if (rewardedAd != null || loading) return
        loading = true
        _status.value = RewardedAdStatus.LOADING
        val adNative: TTAdNative = TTAdSdk.getAdManager().createAdNative(context)
        val adSlot = AdSlot.Builder()
            .setCodeId(CSJ_TEST_REWARD_CODE_ID)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()
        adNative.loadRewardVideoAd(adSlot, object : TTAdNative.RewardVideoAdListener {
            override fun onError(code: Int, msg: String?) {
                Log.w(TAG, "loadRewardVideoAd onError: code=$code msg=$msg")
                rewardedAd = null
                loading = false
                _status.value = RewardedAdStatus.FAILED
            }

            override fun onRewardVideoAdLoad(ad: TTRewardVideoAd) {
                // 物料已下发, 视频缓存完成 (onRewardVideoCached) 后才置 READY
                rewardedAd = ad
            }

            @Deprecated("SDK 兼容回调, 以带参版本为准")
            override fun onRewardVideoCached() = Unit

            override fun onRewardVideoCached(ad: TTRewardVideoAd) {
                rewardedAd = ad
                loading = false
                _status.value = RewardedAdStatus.READY
            }
        })
    }

    override fun show(
        activity: Activity,
        onEarned: () -> Unit,
        onClosed: () -> Unit,
        onError: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onError()
            preload()
            return
        }
        ad.setRewardAdInteractionListener(object : TTRewardVideoAd.RewardAdInteractionListener {
            override fun onAdShow() = Unit

            override fun onAdVideoBarClick() = Unit

            override fun onAdClose() {
                rewardedAd = null
                _status.value = RewardedAdStatus.IDLE
                onClosed()
                preload()
            }

            override fun onVideoComplete() = Unit

            override fun onVideoError() {
                Log.w(TAG, "showRewardVideoAd onVideoError")
                rewardedAd = null
                _status.value = RewardedAdStatus.FAILED
                onError()
            }

            @Deprecated("SDK 兼容回调, 以 onRewardArrived 为准")
            override fun onRewardVerify(
                rewardValid: Boolean,
                rewardType: Int,
                rewardName: String?,
                rewardAmount: Int,
                errorMsg: String?,
            ) = Unit

            override fun onRewardArrived(isRewardValid: Boolean, rewardType: Int, extra: android.os.Bundle?) {
                if (isRewardValid) onEarned()
            }

            override fun onSkippedVideo() = Unit
        })
        _status.value = RewardedAdStatus.LOADING
        ad.showRewardVideoAd(activity)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RewardedAdModule {

    @Binds
    @Singleton
    abstract fun bindRewardedAdController(
        impl: CsjRewardedAdController
    ): RewardedAdController
}
