package com.wanbaohe.minesweeper.component

import com.wanbaohe.minesweeper.logic.Cell
import com.wanbaohe.minesweeper.logic.GameState

data class MinesweeperUiState(
    val board: List<List<Cell>> = emptyList(),
    val gameState: GameState = GameState.INITIAL,
    val timer: Int = 0,
    val minesLeft: Int = 0,
    /** true = 插旗模式(单击即插旗), false = 挖雷模式(单击即翻开) */
    val flagMode: Boolean = false,
    /** 音效与背景音乐总开关 */
    val soundEnabled: Boolean = true
)
