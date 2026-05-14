package com.pathrift.anonve.android.game.towers

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * Blast Tower — AoE explosive. iOS parity.
 * Damage: 15, Speed: 0.5/s, Range: 160px (≈2.5 tiles), AoE: 1.5 tiles
 */
class BlastTower : Tower() {
    override val type = TowerType.BLAST
    override val displayName = "Blast Tower"
    override val cost = EconomyConstants.TowerCost.BLAST
    override val damagePerHit = 15
    override val attacksPerSecond = 0.5f
    override val rangeTiles = 2.0f
    override val aoeRadius = 1.5f
    override val description = "Explosive AoE. Damages all enemies in blast radius."
}
