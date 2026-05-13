package com.pathrift.anonve.android.game.towers

/**
 * Nova Tower — Premium (300 diamonds). Massive AoE explosion. iOS Phase 2.
 * Damage: 90, Speed: 0.35/s, Range: 3.4 tiles, AoE: 2.5 tiles, +50% vs Boss.
 */
class NovaTower : Tower() {
    override val type = TowerType.NOVA
    override val displayName = "Nova"
    override val cost = 500
    override val damagePerHit = 90
    override val attacksPerSecond = 0.35f
    override val rangeTiles = 3.4f
    override val aoeRadius = 2.5f
    override val description = "Massive AoE explosion. Slow fire rate. +50% vs Boss. Unlock: 300♦"
}
