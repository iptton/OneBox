@file:OptIn(ExperimentalFoundationApi::class)

package com.wanbaohe.textcard.presentation.editor.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange

/**
 * 就地编辑选区的样式写操作(文字设置面板选区作用域用)。
 * 局部样式经 Compose 1.12 addStyle 样式层呈现,文字增删由 TrackedRange 自动跟随;
 * 每次应用/清除后经 onChanged 把 state 内容与样式层回写组件模型。
 */

/** 选区字号倍率范围(上下限对齐块级字号滑杆) */
internal const val MIN_SELECTION_SIZE_SCALE = 0.5f
internal const val MAX_SELECTION_SIZE_SCALE = 4f

/** 给当前选区叠加局部样式(追加式:后加覆盖先加),随后回写模型 */
internal fun applySpanStyle(
    state: TextFieldState,
    style: SpanStyle,
    onChanged: () -> Unit,
) {
    val selection = state.selection
    if (selection.collapsed) return
    state.edit { addStyle(style, selection.min, selection.max) }
    onChanged()
}

/**
 * 清除选区局部样式:样式层的 removeStyle/getSpanStyle 等手术 API 是 internal,
 * 公开面无法"按区间删除"——故先经只读 textStyles 快照幸存区间(裁掉选区),
 * 再整体 replace 原文使旧样式区间坍缩,最后回灌幸存段(光标/选区不受影响)。
 */
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
        .filter { it.first != SpanStyle() }
    val text = state.text.toString()
    state.edit {
        replace(0, length, text)
        survivors.forEach { (style, range) -> addStyle(style, range.start, range.end) }
    }
    onChanged()
}

/** 区间裁掉 [start, end) 部分,保留两侧残段(0~2 段) */
private fun TextRange.without(start: Int, end: Int): List<TextRange> = buildList {
    if (min < start) add(TextRange(min, start))
    if (max > end) add(TextRange(end, max))
}
