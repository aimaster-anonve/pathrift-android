package com.pathrift.anonve.android.game.towers

/**
 * Inferno Tower — Premium (50 diamonds). Rapid fire, bypasses Ghost immunity. iOS Phase 2.
 * Damage: 18, Speed: 1.5/s, Range: 2.5 tiles, +75% vs Ghosts.
 */
class InfernoTower : Tower() {
    override val type = TowerType.INFERNO
    override val displayName = "Inferno"
    override val cost = 200
    override val damagePerHit = 18
    override val attacksPerSecond = 1.5f
    override val rangeTiles = 2.5f
    override val description = "Rapid fire. Bypasses Ghost immunity. +75% vs Ghosts. Unlock: 50♦"
}
