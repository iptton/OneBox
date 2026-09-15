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
 * 棋盘规格: 列数固定 9, **行数由屏幕比例算出来**(见 MinesweeperScreen 的 fitBoardRows)。
 *
 * 列数卡在 9 是因为格子保持正方形时, 边长基本等于 (屏宽 - 边距) / 列数 ——
 * 360dp 宽的机器上 10 列只剩 30dp 出头, 指头点下去容易压到邻格; 9 列能到 33dp 以上。
 *
 * 行数为什么不写死: 正方形格子意味着棋盘的宽高比就是 列:行。写死一个行数时,
 * 只要行数偏多, 高度就先撑满, 格子被迫缩小, 棋盘跟着变窄 —— 两边空出一大条,
 * 跟下面的操作栏也对齐不上。所以行数反过来由"能塞几行"决定, 宽度永远吃得满。
 *
 * [MINE_DENSITY] 15.3%, 跟经典 Windows "中级"(15.6%) 基本持平;
 * 行数变了雷数跟着走, 难度手感不随机型漂移。想调难度改这一个数就行。
 */
const val BOARD_COLS = 9

/** 雷的密度。雷数 = 行数 × 列数 × 这个值。 */
const val MINE_DENSITY = 0.153f

/** 行数下限: 再少棋盘就不成竖盘了 */
const val BOARD_ROWS_MIN = 8
/** 行数上限: 再多格子会小到点不准 */
const val BOARD_ROWS_MAX = 18
/** 首帧拿到真实约束前的兜底行数 */
const val BOARD_ROWS_DEFAULT = 14
