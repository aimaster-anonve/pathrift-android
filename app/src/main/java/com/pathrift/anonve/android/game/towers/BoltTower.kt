package com.pathrift.anonve.android.game.towers

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * Bolt Tower — Fast single-target. iOS parity.
 * Damage: 20, Speed: 1.2/s, Range: 192px (≈3 tiles)
 */
class BoltTower : Tower() {
    override val type = TowerType.BOLT
    override val displayName = "Bolt Tower"
    override val cost = EconomyConstants.TowerCost.BOLT
    override val damagePerHit = 20
    override val attacksPerSecond = 1.2f
    override val rangeTiles = 2.4f
    override val description = "Fast electrical strikes. Excellent against light enemies."
}
