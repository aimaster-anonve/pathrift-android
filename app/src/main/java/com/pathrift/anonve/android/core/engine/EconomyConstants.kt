package com.pathrift.anonve.android.core.engine

/**
 * Central registry of all economy constants — iOS parity.
 */
object EconomyConstants {
    const val STARTING_GOLD = 300       // was 250 (Build 5.1 balance)
    const val STARTING_LIVES = 5        // Build 5.2: reverted from 8 back to 5
    const val SELL_REFUND_PERCENT = 0.70
    const val RIFT_SELL_PERCENT = 0.50
    const val UPGRADE_BASE_COST = 80
    const val UPGRADE_GROWTH_RATE = 1.45

    fun goldRewardForWave(wave: Int): Int {
        val base = 55
        val bonus = minOf(wave * 3, 75)  // cap 130 at wave 25+ (was 115 at wave 20+)
        return base + bonus
    }

    /** Kill gold multiplier based on cycle depth — Build 5.1 balance tuning (iOS parity). */
    fun killGoldMultiplier(cycle: Int): Double = when {
        cycle <= 1 -> 1.00   // Cycle 1: full reward
        cycle == 2 -> 0.90   // Cycle 2 (wave 19+): -10% (was -15%)
        cycle == 3 -> 0.82   // Cycle 3 (wave 28+): -18% (was -25%)
        else       -> 0.72   // Cycle 4+ (wave 37+): -28% (was -35%)
    }

    object TowerCost {
        const val BOLT = 80
        const val BLAST = 100   // BUILD7: was 130
        const val FROST = 100
    }

    object EnemyGoldReward {
        const val RUNNER = 6
        const val TANK = 18
        const val SHIELD = 12
        const val SWARM = 3
        const val GHOST = 10
        const val BOSS = 120
        const val SPLITTER = 8
        const val JUMPER = 15
        const val HEALER = 14   // PATHRIFT-159
        const val PHANTOM = 10  // PATHRIFT-159
    }
}
