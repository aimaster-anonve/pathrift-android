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
        val waveBonus = minOf(wave * 3, 60)   // was wave*4, 80 (PATHRIFT-154)
        return base + waveBonus               // new cap: 115 gold/wave (at wave 20+)
    }

    /** Kill gold multiplier based on cycle depth — reduces per-kill income in later cycles (PATHRIFT-154). */
    fun killGoldMultiplier(cycle: Int): Double = when {
        cycle <= 1 -> 1.00   // Cycle 1: full reward
        cycle == 2 -> 0.85   // Cycle 2 (wave 19+): -15%
        cycle == 3 -> 0.75   // Cycle 3 (wave 28+): -25%
        else       -> 0.65   // Cycle 4+ (wave 37+): -35%
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
        const val SPLITTER = 8
        const val JUMPER = 15
        const val HEALER = 14   // PATHRIFT-159
        const val PHANTOM = 10  // PATHRIFT-159
    }
}
