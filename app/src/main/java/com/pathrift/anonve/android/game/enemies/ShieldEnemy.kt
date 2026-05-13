package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * ShieldEnemy — iOS parity.
 * HP: 120, Speed: 85, Armor: none
 * Shield absorbs first 80 damage before HP is reduced.
 */
object ShieldEnemy {
    const val HP = 120f
    const val SPEED = 85f
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.SHIELD
    const val ARMOR_REDUCTION = 0f
    const val SHIELD_HP = 80f
}
