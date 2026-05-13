package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * Tank — Slow, high-HP armored enemy. iOS parity.
 * HP: 300, Speed: 45, Armor: 30%
 */
object TankEnemy {
    const val HP = 300f
    const val SPEED = 45f
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.TANK
    const val ARMOR_REDUCTION = 0.30f
}
