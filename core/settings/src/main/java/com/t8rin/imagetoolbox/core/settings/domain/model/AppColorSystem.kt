package com.t8rin.imagetoolbox.core.settings.domain.model

import com.t8rin.dynamic.theme.ColorSpecVersion
import com.t8rin.dynamic.theme.PaletteStyle

/**
 * 全局色彩系统 —— 调色板风格 / 对比度 / 色彩规范 / Expressive 动效。
 *
 * 与 [AppThemePreset] 的分工: 预设决定"用什么颜色"(色元组 / 动态取色 / 玻璃 / 背景),
 * 色彩系统决定"颜色如何推导"(Material 色彩规范与派生风格), 因此全局唯一、不随预设切换。
 *
 * @param paletteStyle      调色板风格(TonalSpot / Expressive / Vibrant ...)
 * @param contrastLevel     对比度, -1.0(低) ~ 1.0(高), 0.0 为默认
 * @param colorSpec         色彩规范版本(Spec2021 原版 / Spec2025 Material 3 Expressive)
 * @param isExpressiveTheme 是否启用 MaterialExpressiveTheme(表达性动效与形状)
 */
data class AppColorSystem(
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val contrastLevel: Double = 0.0,
    val colorSpec: ColorSpecVersion = ColorSpecVersion.Spec2021,
    val isExpressiveTheme: Boolean = false,
) {
    companion object {
        const val MIN_CONTRAST_LEVEL: Double = -1.0
        const val MAX_CONTRAST_LEVEL: Double = 1.0
    }
}
