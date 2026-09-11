package com.wanbaohe.decisionwheel.data

import com.wanbaohe.decisionwheel.component.WheelSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 转盘设置的进程内单一真相。
 *
 * 设置页改完要立刻反映到 Spin 页（旋转时长、抽后移除等），所以不能各自从 MMKV 各读各的。
 */
@Singleton
class WheelSettingsHolder @Inject constructor() {

    private val _settings = MutableStateFlow(WheelSettingsStorage.load())
    val settings: StateFlow<WheelSettings> = _settings.asStateFlow()

    fun update(settings: WheelSettings) {
        WheelSettingsStorage.save(settings)
        _settings.value = WheelSettingsStorage.load()
    }

    fun current(): WheelSettings = _settings.value
}
