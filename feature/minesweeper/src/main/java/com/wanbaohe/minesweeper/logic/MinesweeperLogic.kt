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
 * 唯一的棋盘规格: 9 列 × 16 行, 22 颗雷。
 *
 * 竖盘。列数卡在 9 是因为格子保持正方形时, 边长基本等于
 * (屏宽 - 边距) / 列数 —— 360dp 宽的机器上 10 列只剩 30dp 出头,
 * 指头点下去容易压到邻格; 9 列能到 33dp, 加上 4dp 间距, 不挤。
 * 行数拉到 16 不是为了加格子, 是为了让整块变成竖向的
 * (9 : 16 ≈ 手机竖屏内容区比例), 一屏放得下, 不用滚。
 * 密度 15.3%, 跟经典 Windows "中级"(15.6%) 基本持平。
 * 想调难度改 [BOARD_MINES] 一个数就行。
 */
const val BOARD_ROWS = 16
const val BOARD_COLS = 9
const val BOARD_MINES = 22
