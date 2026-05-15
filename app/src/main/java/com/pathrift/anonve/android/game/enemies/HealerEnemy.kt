package com.pathrift.anonve.android.game.enemies

/**
 * HealerEnemy — support enemy that heals nearby allies every 2.5s (PATHRIFT-159).
 * Visual: hollow circle (donut) with pulsing green aura ring.
 */
object HealerEnemy {
    const val HP = 140f
    const val SPEED = 35f
    const val GOLD_REWARD = 14
    const val ARMOR_REDUCTION = 0f

    /** Heal amount per aura tick. */
    const val HEAL_AMOUNT = 18f

    /** Aura tick interval in milliseconds. */
    const val HEAL_INTERVAL_MS = 2500L

    /** Heal radius in path-progress units (~abs difference). */
    const val HEAL_RADIUS_PROGRESS = 0.08f
}
