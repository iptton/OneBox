package com.t8rin.dynamic.theme

import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Material 色彩规范版本。
 *
 * - [Spec2021] 原 Material You 规范(MaterialKolor 的默认值)。
 * - [Spec2025] Material 3 Expressive 引入的 2025 色彩规范,配色更饱和、更"表达"。
 *
 * 对应 MaterialKolor Builder 里的 `color_spec` 参数,与 [PaletteStyle.Expressive]
 * 组合即为截图中的 "Material 3 Expressive" 配色。
 */
enum class ColorSpecVersion {
    Spec2021,
    Spec2025;

    companion object {
        /** 兼容用 DataStore / MMKV 存的 ordinal。 */
        fun fromOrdinal(value: Int): ColorSpecVersion = entries.getOrNull(value) ?: Spec2021
    }
}

internal fun ColorSpecVersion.toMaterialKolorSpec(): ColorSpec.SpecVersion = when (this) {
    ColorSpecVersion.Spec2021 -> ColorSpec.SpecVersion.SPEC_2021
    ColorSpecVersion.Spec2025 -> ColorSpec.SpecVersion.SPEC_2025
}
