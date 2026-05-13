package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * GhostEnemy — iOS parity.
 * HP: 90, Speed: 95, Armor: none
 * 90% immune to Frost slow — only 10% of slow effect applies.
 */
object GhostEnemy {
    const val HP = 90f
    const val SPEED = 95f
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.GHOST
    const val ARMOR_REDUCTION = 0f
    /** Fraction of the incoming slow factor that actually applies (10% of slow). */
    const val SLOW_IMMUNITY_FACTOR = 0.10f
}
