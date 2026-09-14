package com.wanbaohe.iching.data

import com.tencent.mmkv.MMKV

/**
 * 六爻音效开关,与起卦记录共用同一个 MMKV 库。
 *
 * 默认开启:摇卦这种强反馈的交互,没声音会明显发闷;不想听的用户一次点击就能关掉。
 */
object IChingSoundSettings {
    private const val STORAGE_ID = "iching_divination"
    private const val KEY_ENABLED = "sound_enabled_v1"

    private val mmkv: MMKV by lazy { MMKV.mmkvWithID(STORAGE_ID) }

    fun isEnabled(): Boolean = runCatching { mmkv.decodeBool(KEY_ENABLED, true) }.getOrDefault(true)

    fun setEnabled(enabled: Boolean) {
        runCatching { mmkv.encode(KEY_ENABLED, enabled) }
    }
}
