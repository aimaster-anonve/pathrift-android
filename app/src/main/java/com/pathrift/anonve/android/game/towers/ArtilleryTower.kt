package com.pathrift.anonve.android.game.towers

/**
 * Artillery Tower — Bridge enemies only, AoE blast, +50% vs Boss.
 * Damage: 55, Speed: 0.4/s, Range: 3.1 tiles, AoE: 1.25 tiles
 */
class ArtilleryTower : Tower() {
    override val type = TowerType.ARTILLERY
    override val displayName = "Artillery"
    override val cost = 160
    override val damagePerHit = 55
    override val attacksPerSecond = 0.4f
    override val rangeTiles = 3.1f
    override val aoeRadius = 1.25f
    override val description = "Bridge enemies only. AoE blast. +50% vs Boss."
}
