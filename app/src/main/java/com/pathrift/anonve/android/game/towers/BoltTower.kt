package com.pathrift.anonve.android.game.towers

/**
 * Bolt Tower — Fast single-target electrical damage.
 * Best for: high-frequency damage on Runners.
 *
 * Stats:
 *   Damage: 20 per hit
 *   Speed: 1.2 attacks/sec
 *   Range: 3 tiles
 *   Cost: 100 gold
 */
class BoltTower : Tower() {
    override val type = TowerType.BOLT
    override val displayName = "Bolt Tower"
    override val cost = 100
    override val damagePerHit = 20
    override val attacksPerSecond = 1.2f
    override val rangeTiles = 3.0f
    override val description = "Fast electrical strikes. Excellent against light enemies."
}
