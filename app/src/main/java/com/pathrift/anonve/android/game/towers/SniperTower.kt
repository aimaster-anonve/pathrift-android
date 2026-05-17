package com.pathrift.anonve.android.game.towers

/**
 * Sniper Tower — Long range, targets all path layers. Single target, high damage, slow fire rate.
 * Damage: 35, Speed: 0.5/s, Range: 4.0 tiles
 */
class SniperTower : Tower() {
    override val type = TowerType.SNIPER
    override val displayName = "Sniper"
    override val cost = 190   // BUILD7: was 220
    override val damagePerHit = 35
    override val attacksPerSecond = 0.5f
    override val rangeTiles = 3.2f
    override val description = "Targets all layers. Long range single target."
}
