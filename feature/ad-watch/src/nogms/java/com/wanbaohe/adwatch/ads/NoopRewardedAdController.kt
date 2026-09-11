package com.wanbaohe.adwatch.ads

import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 非 google 渠道 (国内 + foss) stub: 不打包 AdMob SDK,
 * 入口本就由 channelConfig.enableGms 隐藏 (仅 google 渠道开启),
 * 与 src/google 的真实实现签名保持一致。
 */
@Singleton
class NoopRewardedAdController @Inject constructor() : RewardedAdController {

    override val status: StateFlow<RewardedAdStatus> =
        MutableStateFlow(RewardedAdStatus.UNAVAILABLE).asStateFlow()

    override val isSupported: Boolean = false

    override fun preload() = Unit

    override fun show(
        activity: Activity,
        onEarned: () -> Unit,
        onClosed: () -> Unit,
        onError: () -> Unit,
    ) = onError()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RewardedAdModule {

    @Binds
    @Singleton
    abstract fun bindRewardedAdController(
        impl: NoopRewardedAdController
    ): RewardedAdController
}
