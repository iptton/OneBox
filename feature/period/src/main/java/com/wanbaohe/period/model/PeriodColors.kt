package com.wanbaohe.period.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 经期模块配色 — 全部取自系统主题(Material You 三套配色中的 tertiary 系),
 * 随主题切换自动变化,不写死任何色值。
 */
@Immutable
data class PeriodColors(
    val accent: Color,
    val accentSoft: Color,
    val accentContainer: Color,
    val onAccent: Color,
    val onAccentContainer: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val chartLine: Color,
    val chartBubble: Color,
)

@Composable
fun periodColors(): PeriodColors {
    val scheme = MaterialTheme.colorScheme
    return PeriodColors(
        accent = scheme.tertiary,
        accentSoft = scheme.tertiary.copy(alpha = 0.12f),
        accentContainer = scheme.tertiaryContainer,
        onAccent = scheme.onTertiary,
        onAccentContainer = scheme.onTertiaryContainer,
        background = scheme.background,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        onSurfaceVariant = scheme.onSurfaceVariant,
        chartLine = scheme.tertiary,
        chartBubble = scheme.tertiary,
    )
}
