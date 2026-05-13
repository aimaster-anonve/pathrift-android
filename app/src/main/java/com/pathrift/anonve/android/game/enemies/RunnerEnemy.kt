package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * Runner — Fast, low-HP scout enemy. iOS parity.
 * HP: 50, Speed: 110, Armor: none
 */
object RunnerEnemy {
    const val HP = 50f
    const val SPEED = 110f
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.RUNNER
    const val ARMOR_REDUCTION = 0f
}
