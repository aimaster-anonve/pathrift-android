package com.pathrift.anonve.android.game

import com.pathrift.anonve.android.game.enemies.EnemyType

/**
 * Bridge interface between the game engine and the ViewModel/UI layer.
 * iOS parity — all callbacks match scene.onXxx closures from iOS GameScene.
 */
interface GameBridge {
    fun onWaveStarted(wave: Int, totalEnemies: Int)
    fun onWaveCompleted(wave: Int, goldEarned: Int)
    fun onEnemyKilled(type: EnemyType, gold: Int)
    fun onEnemyEscaped()
    fun onLifeLost(livesRemaining: Int)
    fun onRunEnded(wave: Int, score: Long)
    fun onRiftShift()
    fun onWaveProgress(cleared: Int, total: Int)
    fun onTowerTapped(slotId: Int)
    fun onGoldChanged(gold: Int)
    fun onDiamondsChanged(balance: Int)
    fun onSpeedChanged(multiplier: Float)
    fun onReviveAvailable()
    fun onLifeRestored(lives: Int)
}
