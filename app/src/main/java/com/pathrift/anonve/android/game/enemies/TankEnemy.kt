package com.pathrift.anonve.android.game.enemies

/**
 * Tank — Slow, high-HP armored enemy.
 * Threat: durability. Counter: Blast Tower for AoE, Bolt Tower sustained fire.
 *
 * Stats:
 *   HP: 300
 *   Speed: 50 units/sec
 *   Gold reward: 15
 *   Armor: 20% damage reduction
 */
object TankEnemy {
    const val HP = 300
    const val SPEED = 50f
    const val GOLD_REWARD = 15
    const val ARMOR_REDUCTION = 0.2f
    const val DISPLAY_NAME = "Tank"
    const val DESCRIPTION = "Armored brute. 20% damage reduction. Moves slowly but soaks punishment."
}
