package com.pathrift.anonve.android.game.towers

/**
 * Frost Tower — Low damage, high utility: slows enemy movement by 40%.
 * Best for: creating choke points, enabling other towers to deal more damage.
 *
 * Stats:
 *   Damage: 5 per hit
 *   Speed: 0.8 attacks/sec
 *   Range: 3 tiles
 *   Slow: 40% speed reduction
 *   Cost: 120 gold
 */
class FrostTower : Tower() {
    override val type = TowerType.FROST
    override val displayName = "Frost Tower"
    override val cost = 120
    override val damagePerHit = 5
    override val attacksPerSecond = 0.8f
    override val rangeTiles = 3.0f
    override val slowFactor = 0.6f
    override val description = "Chilling aura slows enemies by 40%. Low damage, high utility."
}
