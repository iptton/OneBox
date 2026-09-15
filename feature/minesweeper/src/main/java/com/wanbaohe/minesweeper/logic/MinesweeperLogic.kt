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
 * 唯一的棋盘规格: 10 列 × 18 行, 28 颗雷。
 *
 * 密度 15.6%, 跟经典 Windows "中级" 一致, 难度手感没打折。
 * 之所以是这个形状: 格子保持正方形时, 高度必然被宽度卡住,
 * 10 : 18 的长宽比刚好贴合手机竖屏的内容区(大约 1 : 1.8),
 * 一屏放得下且每格还能有 31dp 左右 —— 低于 26dp 手指就点不准了。
 */
const val BOARD_ROWS = 18
const val BOARD_COLS = 10
const val BOARD_MINES = 28
