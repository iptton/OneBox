@file:OptIn(ExperimentalFoundationApi::class)

package com.wanbaohe.textcard.presentation.editor.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnit

/**
 * 就地编辑选区的样式写操作(文字设置面板选区作用域用)。
 * 局部样式经 Compose 1.12 addStyle 样式层呈现,文字增删由 TrackedRange 自动跟随;
 * 每次应用/清除后经 onChanged 把 state 内容与样式层回写组件模型。
 *
 * 关键不变量:**同属性区间必须互斥**。em 字号在 Android 端会被转成
 * android.text.style.RelativeSizeSpan(SpannableExtensions.setFontSize),
 * 重叠区间是 textSize 相乘而不是覆盖——编辑器、预览、导出全部走 android.text,
 * 拖一次滑杆的几十个中间值若各自追加一条区间,字号会成倍爆炸。
 * 所以应用/清除都走"快照 → 剥离选区同属性 → 重建样式层"的路径
 * (replace 全文使旧区间坍缩移除,再回灌幸存段;replace 会把选区坍缩到
 * 替换末尾(adjustTextRange),故在 edit 内显式恢复选区——文本未变,下标不变)。
 */

/** 选区字号倍率范围(上下限对齐块级字号滑杆) */
internal const val MIN_SELECTION_SIZE_SCALE = 0.5f
internal const val MAX_SELECTION_SIZE_SCALE = 4f

/** 给当前选区应用局部样式:先剥离选区重叠部分的同属性旧样式,再追加新样式,最后回写模型 */
internal fun applySpanStyle(
    state: TextFieldState,
    style: SpanStyle,
    onChanged: () -> Unit,
) {
    val selection = state.selection
    if (selection.collapsed) return
    val survivors = state.textStyles.getSpanStyles(TextRange(0, state.text.length))
        .flatMap { range -> range.withoutAttribute(selection, style) }
    rebuildStyles(state, survivors, selection) {
        addStyle(style, selection.min, selection.max)
    }
    onChanged()
}

/** 清除选区全部局部样式(选区重叠部分整段剥掉,选区外保留),随后回写模型 */
internal fun clearSelectionStyles(
    state: TextFieldState,
    onChanged: () -> Unit,
) {
    val selection = state.selection
    if (selection.collapsed) return
    val survivors = state.textStyles.getSpanStyles(TextRange(0, state.text.length))
        .flatMap { range ->
            TextRange(range.start, range.end)
                .without(selection.min, selection.max)
                .map { range.item to it }
        }
    rebuildStyles(state, survivors, selection)
    onChanged()
}

/**
 * 重建样式层:replace 全文(内容不变)使旧样式区间全部坍缩移除,回灌幸存区间;
 * 全空样式(四字段均未设置)不再回灌。末尾恢复选区。
 */
private inline fun rebuildStyles(
    state: TextFieldState,
    survivors: List<Pair<SpanStyle, TextRange>>,
    selection: TextRange,
    crossinline extra: androidx.compose.foundation.text.input.TextFieldBuffer.() -> Unit = {},
) {
    val text = state.text.toString()
    state.edit {
        replace(0, length, text)
        survivors.forEach { (style, range) ->
            if (style != SpanStyle()) addStyle(style, range.start, range.end)
        }
        extra()
        this.selection = selection
    }
}

/**
 * 区间按选区切分:选区外部分原样保留;选区内部分剥掉 [newStyle] 设置的属性
 * (newStyle 里设置了的字段 = 本次要替换的属性,剥成未设置即继承块级)。
 */
private fun AnnotatedString.Range<SpanStyle>.withoutAttribute(
    selection: TextRange,
    newStyle: SpanStyle,
): List<Pair<SpanStyle, TextRange>> = buildList {
    TextRange(start, end).without(selection.min, selection.max)
        .forEach { add(item to it) }
    TextRange(start, end).intersectRange(selection.min, selection.max)
        ?.let { add(item.strippedOf(newStyle) to it) }
}

/** 剥掉 [other] 中设置了的四个属性(颜色/字号/粗/斜),其余保留 */
private fun SpanStyle.strippedOf(other: SpanStyle): SpanStyle = SpanStyle(
    color = if (other.color != Color.Unspecified) Color.Unspecified else color,
    fontSize = if (other.fontSize != TextUnit.Unspecified) TextUnit.Unspecified else fontSize,
    fontWeight = if (other.fontWeight != null) null else fontWeight,
    fontStyle = if (other.fontStyle != null) null else fontStyle,
)

/** 区间裁掉 [start, end) 部分,保留两侧残段(0~2 段) */
private fun TextRange.without(start: Int, end: Int): List<TextRange> = buildList {
    if (min < start) add(TextRange(min, start))
    if (max > end) add(TextRange(end, max))
}

/** 区间与 [start, end) 的交集,不相交返回 null */
private fun TextRange.intersectRange(start: Int, end: Int): TextRange? {
    val s = maxOf(min, start)
    val e = minOf(max, end)
    return if (s < e) TextRange(s, e) else null
}
