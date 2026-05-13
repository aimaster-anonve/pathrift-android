package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * Game phase — mirrors iOS GamePhase.
 */
enum class GamePhase {
    PRE_WAVE,
    WAVE_ACTIVE,
    DECISION,
    RIFT_SHIFT,
    GAME_OVER
}

/**
 * Snapshot of a tower slot for UI rendering.
 */
data class TowerSlotData(
    val type: TowerType,
    val level: Int,
    val totalInvested: Int
)

/**
 * Tower info shown in selection panel — mirrors iOS TowerInfo.
 */
data class TowerInfo(
    val slotId: Int,
    val type: TowerType,
    val level: Int,
    val damage: Float,
    val range: Float,
    val attackSpeed: Float,
    val sellValue: Int,
    val upgradeCost: Int
)

/**
 * Full UI game state — iOS parity with GameViewModel @Published vars.
 */
data class GameState(
    val wave: Int = 0,
    val lives: Int = EconomyConstants.STARTING_LIVES,
    val gold: Int = EconomyConstants.STARTING_GOLD,
    val score: Long = 0L,
    val enemyKills: Int = 0,
    val phase: GamePhase = GamePhase.PRE_WAVE,
    val isGameOver: Boolean = false,
    val waveEnemyTotal: Int = 1,
    val waveEnemiesCleared: Int = 0,
    val riftShiftActive: Boolean = false,
    val selectedTowerSlotId: Int? = null,
    val selectedTowerInfo: TowerInfo? = null,
    val waveCompleteMessage: String? = null,
    val activeTowerSlots: Map<Int, TowerSlotData> = emptyMap(),
    val diamonds: Int = 0,
)
