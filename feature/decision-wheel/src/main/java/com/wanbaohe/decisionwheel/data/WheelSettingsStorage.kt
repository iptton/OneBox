package com.wanbaohe.decisionwheel.data

import com.tencent.mmkv.MMKV
import com.wanbaohe.decisionwheel.component.WheelSettings

/**
 * 转盘设置的轻量持久化（MMKV）。
 *
 * 之前这些值存在 Screen 的 remember 里，重建即丢；改为持久化后跨进程重建保留。
 */
object WheelSettingsStorage {

    private const val KEY_DURATION = "spin_duration_millis"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_HAPTICS = "haptics_enabled"
    private const val KEY_REMOVE_ON_SPIN = "remove_on_spin"

    private val mmkv: MMKV get() = MMKV.mmkvWithID("decision_wheel")

    fun load(): WheelSettings = WheelSettings(
        spinDurationMillis = mmkv.decodeInt(KEY_DURATION, 4000).coerceIn(1500, 8000),
        soundEnabled = mmkv.decodeBool(KEY_SOUND, true),
        hapticsEnabled = mmkv.decodeBool(KEY_HAPTICS, true),
        removeOnSpin = mmkv.decodeBool(KEY_REMOVE_ON_SPIN, false)
    )

    fun save(settings: WheelSettings) {
        mmkv.encode(KEY_DURATION, settings.spinDurationMillis.coerceIn(1500, 8000))
        mmkv.encode(KEY_SOUND, settings.soundEnabled)
        mmkv.encode(KEY_HAPTICS, settings.hapticsEnabled)
        mmkv.encode(KEY_REMOVE_ON_SPIN, settings.removeOnSpin)
    }
}
