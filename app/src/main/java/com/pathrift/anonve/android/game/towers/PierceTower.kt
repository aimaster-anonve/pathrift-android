package com.pathrift.anonve.android.game.towers

/**
 * Pierce Tower — Hits all enemies in range, bypasses shields. iOS Phase 2.
 * Damage: 25, Speed: 1.0/s, Range: 3.0 tiles, +100% vs Shields.
 */
class PierceTower : Tower() {
    override val type = TowerType.PIERCE
    override val displayName = "Pierce"
    override val cost = 130
    override val damagePerHit = 25
    override val attacksPerSecond = 1.0f
    override val rangeTiles = 3.0f
    override val description = "Pierces shields. Hits all enemies in range. +100% vs Shields."
}
