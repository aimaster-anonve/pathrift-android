package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.game.enemies.EnemyInstance
import com.pathrift.anonve.android.game.towers.Tower

/**
 * Internal game simulation state.
 * Separate from UiGameState which is used by Compose layer.
 */
data class GameState(
    val wave: Int = 0,
    val lives: Int = 3,
    val gold: Int = 150,
    val score: Long = 0L,
    val towers: Map<TileCoordinate, Tower> = emptyMap(),
    val enemies: List<EnemyInstance> = emptyList()
)
