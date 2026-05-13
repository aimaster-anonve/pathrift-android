package com.pathrift.anonve.android.game.towers

import com.pathrift.anonve.android.core.engine.EconomyConstants

/**
 * Frost Tower — Slows 40%. iOS parity.
 * Damage: 5, Speed: 0.8/s, Range: 192px (≈3 tiles), Slow: 40%
 */
class FrostTower : Tower() {
    override val type = TowerType.FROST
    override val displayName = "Frost Tower"
    override val cost = EconomyConstants.TowerCost.FROST
    override val damagePerHit = 5
    override val attacksPerSecond = 0.8f
    override val rangeTiles = 3.0f
    override val slowFactor = 0.6f  // 1.0 - 0.40 = 0.60 → enemy speed *= 0.60 → 40% slower
    override val description = "Chilling aura slows enemies 40%. Low damage, high utility."
}
