package com.pathrift.anonve.android.game.towers

/**
 * Blast Tower — Slow-firing AoE explosive damage.
 * Best for: clearing groups of Runners, softening Tank clusters.
 *
 * Stats:
 *   Damage: 15 per hit (AoE radius 1.5 tiles)
 *   Speed: 0.5 attacks/sec
 *   Range: 2.5 tiles
 *   Cost: 150 gold
 */
class BlastTower : Tower() {
    override val type = TowerType.BLAST
    override val displayName = "Blast Tower"
    override val cost = 150
    override val damagePerHit = 15
    override val attacksPerSecond = 0.5f
    override val rangeTiles = 2.5f
    override val aoeRadius = 1.5f
    override val description = "Explosive AoE. Damages all enemies in blast radius."
}
