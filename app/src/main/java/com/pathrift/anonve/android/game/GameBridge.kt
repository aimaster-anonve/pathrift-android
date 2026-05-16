package com.pathrift.anonve.android.game

import android.graphics.PointF
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.TowerType

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
    fun onStateRestored(wave: Int, lives: Int, gold: Int, kills: Int)
    /** Called each time a tower fires — drives the projectile visual effect. */
    fun onProjectileFired(from: PointF, to: PointF, type: TowerType) {}
}
