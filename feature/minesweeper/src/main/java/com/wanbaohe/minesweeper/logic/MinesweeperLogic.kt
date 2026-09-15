package com.wanbaohe.minesweeper.logic

data class Cell(
    val row: Int,
    val col: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val neighborMines: Int = 0
)

enum class GameState {
    INITIAL, PLAYING, WON, LOST
}

/**
 * 唯一的棋盘规格: 9 × 9, 14 颗雷。
 *
 * 正方形盘, 行列数一致。定 9 列是因为格子保持正方形时, 边长基本等于
 * (屏宽 - 边距) / 列数 —— 360dp 宽的机器上一列 10 格只有 30dp 出头,
 * 减到 9 列能到 33-34dp, 加上 4dp 间距, 不再挤成一团。
 * 密度 17.3%, 比经典中级(15.6%)略辣一点, 免得格子变少后游戏太短。
 * 想调难度改 [BOARD_MINES] 一个数就行。
 */
const val BOARD_ROWS = 9
const val BOARD_COLS = 9
const val BOARD_MINES = 14
