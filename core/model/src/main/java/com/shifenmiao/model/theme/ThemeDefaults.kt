package com.shifenmiao.model.theme

/**
 * 主题外观的共享默认值。
 *
 * 放在 core:model 是因为 :core:storage、:core:settings、:core:database、:core:ui 都依赖它，
 * 而这些模块各自持有同一份默认值（DataStore、主题预设、冷启动快照、Room 建表/迁移），
 * 单一常量可以避免"改了一处、漏了另一处"导致新装与升级表现不一致。
 */
object ThemeDefaults {

    /**
     * 玻璃描边可见度默认值 (0f..1f)，0 隐藏描边，1 为完整强度。
     *
     * 注意: ThemePresetEntity 的 @ColumnInfo(defaultValue = "0.17") 是注解实参，
     * 必须是字面量，无法引用本常量，修改时请一并同步。
     */
    const val DEFAULT_GLASS_BORDER_ALPHA = 0.17f
}
