package com.pathrift.anonve.android.game.towers

/**
 * Tesla Tower — Premium (150 diamonds). Chain lightning hits 3 targets. iOS Phase 2.
 * Damage: 35, Speed: 0.9/s, Range: 3.1 tiles, +75% vs Swarms.
 */
class TeslaTower : Tower() {
    override val type = TowerType.TESLA
    override val displayName = "Tesla"
    override val cost = 300
    override val damagePerHit = 35
    override val attacksPerSecond = 0.9f
    override val rangeTiles = 3.1f
    override val description = "Chain lightning hits 3 targets. +75% vs Swarms. Unlock: 150♦"
}
