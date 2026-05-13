package com.pathrift.anonve.android.game.enemies

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * BossEnemy — iOS parity. 5 variants cycling every 10 waves.
 * HP scales with wave number. Armor and speed differ per variant.
 */
object BossEnemy {
    const val GOLD_REWARD = EconomyConstants.EnemyGoldReward.BOSS

    /** Variant index: (waveNumber / 10 - 1) % 5 */
    fun variantIndex(waveNumber: Int): Int = (waveNumber / 10 - 1) % 5

    fun hp(waveNumber: Int): Float {
        val cycle = waveNumber / 50
        return (800 + (waveNumber / 10) * 300 + cycle * 500).toFloat()
    }

    fun armor(variantIndex: Int): Float = when (variantIndex) {
        1 -> 0.50f
        2 -> 0.10f
        3 -> 0.05f
        4 -> 0.60f
        else -> 0.30f   // variant 0: Rift Guardian
    }

    fun speed(variantIndex: Int): Float = when (variantIndex) {
        1 -> 22f
        2 -> 50f
        3 -> 70f
        4 -> 18f
        else -> 32f     // variant 0: Rift Guardian
    }
}
