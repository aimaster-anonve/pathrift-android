package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * SwarmEnemy — iOS parity.
 * HP: 25, Speed: 120, Armor: none
 * Fast packs, low HP, low gold reward.
 */
object SwarmEnemy {
    const val HP = 25f
    const val SPEED = 120f
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.SWARM
    const val ARMOR_REDUCTION = 0f
}
