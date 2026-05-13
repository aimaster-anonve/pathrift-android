package com.pathrift.anonve.android.game.enemies

/**
 * Runner — Fast, low-HP scout enemy.
 * Threat: speed. Counter: Bolt Tower or Frost Tower to slow + kill.
 *
 * Stats:
 *   HP: 50
 *   Speed: 150 units/sec
 *   Gold reward: 5
 *   Armor: none
 */
object RunnerEnemy {
    const val HP = 50
    const val SPEED = 150f
    const val GOLD_REWARD = 5
    const val ARMOR_REDUCTION = 0f
    const val DISPLAY_NAME = "Runner"
    const val DESCRIPTION = "Fast scout. Low HP, no armor. Overwhelms in numbers."
}
