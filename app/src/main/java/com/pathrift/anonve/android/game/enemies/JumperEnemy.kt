package com.pathrift.anonve.android.game.enemies

/**
 * JumperEnemy — teleports forward every 3 seconds. iOS Phase 2.
 */
object JumperEnemy {
    const val HP = 120f
    const val SPEED = 55f
    const val GOLD_REWARD = 15
    const val ARMOR_REDUCTION = 0.10f
    const val JUMP_INTERVAL_MS = 5000L  // was 3000L (PATHRIFT-153)
    const val JUMP_DISTANCE = 0.10f    // was 0.20f (PATHRIFT-153)
}
