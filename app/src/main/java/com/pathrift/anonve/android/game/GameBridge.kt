package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.game.enemies.EnemyType

/**
 * Bridge interface between the game simulation layer and the Compose/ViewModel layer.
 * The game engine calls these methods to report state changes back to the ViewModel.
 */
interface GameBridge {
    fun onWaveStarted(wave: Int)
    fun onWaveCompleted(wave: Int, goldEarned: Int)
    fun onEnemyKilled(type: EnemyType, gold: Int)
    fun onLifeLost(livesRemaining: Int)
    fun onRunEnded(wave: Int, score: Long)
}
