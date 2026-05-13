package com.pathrift.anonve.android.core.engine

/**
 * Central registry of all economy constants — iOS parity.
 */
object EconomyConstants {
    const val STARTING_GOLD = 250
    const val STARTING_LIVES = 5
    const val SELL_REFUND_PERCENT = 0.70
    const val RIFT_SELL_PERCENT = 0.50
    const val UPGRADE_BASE_COST = 80
    const val UPGRADE_GROWTH_RATE = 1.45

    fun goldRewardForWave(wave: Int): Int {
        val base = 55
        val waveBonus = minOf(wave * 4, 80)
        return base + waveBonus
    }

    object TowerCost {
        const val BOLT = 80
        const val BLAST = 130
        const val FROST = 100
    }

    object EnemyGoldReward {
        const val RUNNER = 6
        const val TANK = 18
        const val SHIELD = 12
        const val SWARM = 3
        const val GHOST = 10
        const val BOSS = 120
    }
}
