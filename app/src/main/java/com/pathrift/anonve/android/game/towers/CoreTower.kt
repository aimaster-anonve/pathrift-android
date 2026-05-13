package com.pathrift.anonve.android.game.towers

/**
 * Core Tower — Armor penetration 50%. High single-target damage. iOS Phase 2.
 * Damage: 45, Speed: 0.7/s, Range: 2.5 tiles, +60% vs Tanks.
 */
class CoreTower : Tower() {
    override val type = TowerType.CORE
    override val displayName = "Core"
    override val cost = 180
    override val damagePerHit = 45
    override val attacksPerSecond = 0.7f
    override val rangeTiles = 2.5f
    override val description = "Armor penetration 50%. High damage. +60% vs Tanks."
}
