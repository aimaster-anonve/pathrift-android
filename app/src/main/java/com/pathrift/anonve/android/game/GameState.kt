package com.pathrift.anonve.android.game

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.game.towers.TowerType

/**
 * In-flight projectile — tracks a fired projectile from tower to enemy.
 * Damage is applied only when the projectile arrives (PATHRIFT-B7-F001).
 */
data class InFlightProjectile(
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
    val fromPos: PointF,
    val toPos: PointF,
    val towerType: TowerType,
    val targetEnemyId: Long,
    val rawDamage: Int,
    val bypassShield: Boolean = false,
    val penetration: Float = 0f,
    val isAoe: Boolean = false,
    val aoeRadius: Float = 0f,
    val chainTargets: List<Pair<Long, Int>> = emptyList(), // enemyId → damage
    val spawnTimeMs: Long = System.currentTimeMillis()
)

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
    val upgradeCost: Int,
    val moveCost: Int = 0,        // BUILD7: ceil(totalInvested * 0.30)
    val totalInvested: Int = 0    // BUILD7: needed for moveCost display
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
    val speedMultiplier: Float = 1.0f,
    val showRevivePrompt: Boolean = false,
    val reviveCountdown: Int = 5,
    val layoutVersion: Int = 0,  // incremented on initLayout → forces slot recomposition
    // Inter-wave countdown (PATHRIFT-B7-002)
    val interWaveSecondsRemaining: Int = 0,
    // Drag-and-drop state (PATHRIFT-B7-004)
    val isDraggingTower: Boolean = false,
    val dragTowerType: TowerType? = null,
    val dragPosition: Offset = Offset.Zero,
    val dragValidSlotId: Int? = null,
    val isMovingTower: Boolean = false,
    val movingFromSlotId: Int? = null,
    val moveCost: Int = 0,
)
