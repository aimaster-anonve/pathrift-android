package com.pathrift.anonve.android.game.enemies

/**
 * PhantomEnemy — evasion enemy with 40% chance to dodge single-target projectiles (PATHRIFT-159).
 * AoE attacks always connect. Visual: semi-transparent violet 5-pointed star.
 */
object PhantomEnemy {
    const val HP = 90f
    const val SPEED = 60f
    const val GOLD_REWARD = 10
    const val ARMOR_REDUCTION = 0f

    /** Probability that a single-target hit is dodged. */
    const val DODGE_CHANCE = 0.40
}
